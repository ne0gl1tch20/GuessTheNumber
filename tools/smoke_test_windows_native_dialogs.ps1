param(
    [Parameter(Mandatory = $true)]
    [string]$MsiPath
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path -LiteralPath $MsiPath)) { throw "MSI not found: $MsiPath" }

Write-Host "Installing MSI for native-window smoke test..."
$install = Start-Process -FilePath "msiexec.exe" -ArgumentList ('/i "' + $MsiPath + '" /qn /norestart') -Wait -PassThru
if ($install.ExitCode -notin @(0, 3010)) { throw "MSI install failed with exit code $($install.ExitCode)." }

$uninstallRoots = @(
    "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*",
    "HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*",
    "HKLM:\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*"
)
$entry = @(
    $uninstallRoots |
        ForEach-Object { Get-ItemProperty -Path $_ -ErrorAction SilentlyContinue } |
        Where-Object { $_.DisplayName -eq "GuessTheNumber" }
) | Select-Object -First 1
if (-not $entry) { throw "Could not locate the installed GuessTheNumber MSI entry." }

$launcherPath = $null
if ($entry.DisplayIcon) {
    $iconPath = ([string]$entry.DisplayIcon -replace ',\d+$', '').Trim('"')
    if (Test-Path -LiteralPath $iconPath) { $launcherPath = $iconPath }
}
if (-not $launcherPath -and $entry.InstallLocation) {
    $candidate = Join-Path $entry.InstallLocation "GuessTheNumber.exe"
    if (Test-Path -LiteralPath $candidate) { $launcherPath = $candidate }
}
if (-not $launcherPath) {
    $candidates = @(
        "$env:LOCALAPPDATA\GuessTheNumber\GuessTheNumber.exe",
        "$env:LOCALAPPDATA\Programs\GuessTheNumber\GuessTheNumber.exe",
        "$env:ProgramFiles\GuessTheNumber\GuessTheNumber.exe"
    )
    $launcherPath = $candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
}
if (-not $launcherPath) { throw "Could not locate GuessTheNumber.exe from the installed MSI." }
Write-Host "Installed launcher: $launcherPath"

if (-not ("GTNNativeWindowProbe" -as [type])) {
    Add-Type -TypeDefinition @"
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;

public static class GTNNativeWindowProbe {
    public sealed class WindowInfo {
        public string Title { get; set; }
        public uint ProcessId { get; set; }
    }
    private delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);
    [DllImport("user32.dll")]
    private static extern bool EnumWindows(EnumWindowsProc callback, IntPtr lParam);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern int GetWindowText(IntPtr hWnd, StringBuilder text, int maxCount);
    [DllImport("user32.dll")]
    private static extern bool IsWindowVisible(IntPtr hWnd);
    [DllImport("user32.dll")]
    private static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint processId);

    public static List<WindowInfo> GetVisibleWindows() {
        var result = new List<WindowInfo>();
        EnumWindows((hWnd, lParam) => {
            if (!IsWindowVisible(hWnd)) return true;
            var text = new StringBuilder(512);
            if (GetWindowText(hWnd, text, text.Capacity) <= 0) return true;
            uint processId;
            GetWindowThreadProcessId(hWnd, out processId);
            result.Add(new WindowInfo { Title = text.ToString(), ProcessId = processId });
            return true;
        }, IntPtr.Zero);
        return result;
    }
}
"@
}

function Stop-GtnWindowProcess([uint32]$ProcessId) {
    Get-Process -Id $ProcessId -ErrorAction SilentlyContinue |
        Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

function Assert-NativeDialog([string]$ExpectedTitle) {
    Write-Host "Launching app and waiting for native dialog: $ExpectedTitle"
    $launcher = Start-Process -FilePath $launcherPath -PassThru
    $deadline = (Get-Date).AddSeconds(45)
    $found = $null
    while ((Get-Date) -lt $deadline) {
        $found = [GTNNativeWindowProbe]::GetVisibleWindows() |
            Where-Object { $_.Title -eq $ExpectedTitle } |
            Select-Object -First 1
        if ($found) { break }
        if ($launcher.HasExited) {
            throw "The application exited before showing '$ExpectedTitle' (exit code $($launcher.ExitCode))."
        }
        Start-Sleep -Seconds 1
    }
    if (-not $found) {
        $titles = [GTNNativeWindowProbe]::GetVisibleWindows() |
            ForEach-Object { "'$($_.Title)' (PID $($_.ProcessId))" }
        throw "Native dialog '$ExpectedTitle' did not appear. Visible windows: $($titles -join ', ')"
    }
    Write-Host "PASS: found native window '$($found.Title)' (PID $($found.ProcessId))."
    Stop-GtnWindowProcess -ProcessId $found.ProcessId
}

$preferencesRoot = "HKCU:\Software\JavaSoft\Prefs\com\jarrlyyy\guessthenumber\desktop"
try {
    Remove-Item -LiteralPath $preferencesRoot -Recurse -Force -ErrorAction SilentlyContinue
    Assert-NativeDialog -ExpectedTitle "Choose Profile Picture"

    $profileNode = Join-Path $preferencesRoot "player-profile"
    $startupNode = Join-Path $preferencesRoot "startup-setup"
    New-Item -Path $profileNode -Force | Out-Null
    New-Item -Path $startupNode -Force | Out-Null
    $profileJson = '{"username":"CI Player","bio":"","avatarId":"star","customAvatarPath":null,"setupComplete":true,"profileId":"ci-native-dialog-smoke"}'
    Set-ItemProperty -LiteralPath $profileNode -Name "profile_json" -Value $profileJson
    Set-ItemProperty -LiteralPath $startupNode -Name "profile_and_save_slot_setup_complete" -Value "false"

    Assert-NativeDialog -ExpectedTitle "Choose a Save Slot"
    Write-Host "PASS: both mandatory first-boot dialogs opened as separate native windows."
}
finally {
    Get-CimInstance Win32_Process -Filter "name = 'GuessTheNumber.exe'" -ErrorAction SilentlyContinue |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
    Remove-Item -LiteralPath $preferencesRoot -Recurse -Force -ErrorAction SilentlyContinue
    if ($entry -and $entry.PSChildName) {
        $uninstall = Start-Process -FilePath "msiexec.exe" -ArgumentList ('/x ' + $entry.PSChildName + ' /qn /norestart') -Wait -PassThru
        if ($uninstall.ExitCode -notin @(0, 1605, 3010)) {
            Write-Warning "Smoke-test MSI cleanup returned exit code $($uninstall.ExitCode)."
        }
    }
}
