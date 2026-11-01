#define MyAppName "GuessTheNumber"
#define MyAppPublisher "Jarrlyy"
#ifndef AppVersion
  #define AppVersion "1.0.0"
#endif
#ifndef AppImageDir
  #error "AppImageDir must point to the jpackage app-image directory."
#endif
#ifndef AppIcon
  #error "AppIcon must point to the generated Windows icon."
#endif
#ifndef OutputDir
  #define OutputDir "Output"
#endif

[Setup]
AppId={{6178B622-7859-39F2-8ED0-E0466C715DD1}
AppName={#MyAppName}
AppVersion={#AppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={autopf}\GuessTheNumber
DefaultGroupName=GuessTheNumber
UninstallDisplayName=GuessTheNumber
UninstallDisplayIcon={app}\GuessTheNumber.exe
SetupIconFile={#AppIcon}
OutputDir={#OutputDir}
OutputBaseFilename=GuessTheNumber-{#AppVersion}-Setup
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
PrivilegesRequired=admin
PrivilegesRequiredOverridesAllowed=dialog
WizardStyle=modern
Compression=lzma2
SolidCompression=yes
CloseApplications=yes
RestartApplications=no
DisableProgramGroupPage=yes
UsePreviousAppDir=yes
VersionInfoVersion={#AppVersion}.0
VersionInfoProductName=GuessTheNumber
VersionInfoCompany={#MyAppPublisher}
SetupLogging=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional shortcuts:"; Flags: unchecked

[Files]
Source: "{#AppImageDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\GuessTheNumber"; Filename: "{app}\GuessTheNumber.exe"; WorkingDir: "{app}"
Name: "{autodesktop}\GuessTheNumber"; Filename: "{app}\GuessTheNumber.exe"; WorkingDir: "{app}"; Tasks: desktopicon

[Run]
Filename: "{app}\GuessTheNumber.exe"; Description: "Launch GuessTheNumber"; Flags: postinstall nowait skipifsilent

[Code]
function FindLegacyMsiUninstallString(RootKey: Integer; UninstallRoot: String): String;
var
  Subkeys: TArrayOfString;
  Index: Integer;
  DisplayName: String;
  UninstallString: String;
begin
  Result := '';
  if not RegGetSubkeyNames(RootKey, UninstallRoot, Subkeys) then
    exit;
  for Index := 0 to GetArrayLength(Subkeys) - 1 do
  begin
    DisplayName := '';
    UninstallString := '';
    if RegQueryStringValue(RootKey, UninstallRoot + '\' + Subkeys[Index], 'DisplayName', DisplayName) and
       (Pos('GuessTheNumber', DisplayName) > 0) and
       RegQueryStringValue(RootKey, UninstallRoot + '\' + Subkeys[Index], 'UninstallString', UninstallString) and
       (Pos('msiexec', Lowercase(UninstallString)) > 0) then
    begin
      Result := UninstallString;
      exit;
    end;
  end;
end;

function RemoveLegacyMsiInstall(): Boolean;
var
  UninstallString: String;
  ProductCode: String;
  GuidStart: Integer;
  GuidEnd: Integer;
  ResultCode: Integer;
begin
  Result := True;
  UninstallString := FindLegacyMsiUninstallString(HKLM64, 'Software\Microsoft\Windows\CurrentVersion\Uninstall');
  if UninstallString = '' then
    UninstallString := FindLegacyMsiUninstallString(HKLM32, 'Software\Microsoft\Windows\CurrentVersion\Uninstall');
  if UninstallString = '' then
    UninstallString := FindLegacyMsiUninstallString(HKCU, 'Software\Microsoft\Windows\CurrentVersion\Uninstall');
  if UninstallString = '' then
    exit;

  GuidStart := Pos('{', UninstallString);
  GuidEnd := Pos('}', UninstallString);
  if (GuidStart = 0) or (GuidEnd <= GuidStart) then
  begin
    MsgBox('The previous MSI installation was detected, but its product code could not be read. Your existing installation was left untouched.', mbError, MB_OK);
    Result := False;
    exit;
  end;

  ProductCode := Copy(UninstallString, GuidStart, GuidEnd - GuidStart + 1);
  if not Exec(ExpandConstant('{sys}\msiexec.exe'), '/x ' + ProductCode + ' /qn /norestart', '', SW_HIDE, ewWaitUntilTerminated, ResultCode) or
     not ((ResultCode = 0) or (ResultCode = 1605) or (ResultCode = 3010)) then
  begin
    MsgBox('GuessTheNumber could not remove the previous MSI installation. No application files were replaced. Your saves in AppData remain untouched.', mbError, MB_OK);
    Result := False;
  end;
end;

function InitializeSetup(): Boolean;
begin
  Result := RemoveLegacyMsiInstall();
end;
