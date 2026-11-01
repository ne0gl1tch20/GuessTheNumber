import buildlogic.GenerateDesktopLogoAssets

import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val windowsPackageVersion = providers.environmentVariable("GTN_WINDOWS_PACKAGE_VERSION")
    .orElse("1.0.0")

val appLogoPng = rootProject.file("GTN_APP_LOGO.png")
val generatedDesktopResources = layout.buildDirectory.dir("generated/desktopLogoResources")
val generatedWindowsIcon = layout.buildDirectory.file("generated/windowsIcon/GTN_APP_LOGO.ico")
val generatedDesktopBuildConfig = layout.buildDirectory.dir("generated/desktopBuildConfig")
val desktopDebugToolsBuildAllowed = providers.gradleProperty("gtn.desktop.debugToolsBuild")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(false)
val desktopLiveOpsManifestUrl = providers.gradleProperty("luaLiveOpsManifestUrl")
    .orElse("https://raw.githubusercontent.com/ne0gl1tch20/GuessTheNumber-LiveOps/main/manifest.json")
val desktopLiveOpsPublicKey = providers.gradleProperty("luaLiveOpsPublicKeyBase64")
    .map { value -> value.filterNot(Char::isWhitespace) }
    .orElse("")
val generateDesktopBuildConfig = tasks.register<org.gradle.api.tasks.WriteProperties>("generateDesktopBuildConfig") {
    destinationFile = generatedDesktopBuildConfig.get().file("desktop-build.properties").asFile
    property("debugToolsBuildAllowed", desktopDebugToolsBuildAllowed.get().toString())
    property("liveOpsManifestUrl", desktopLiveOpsManifestUrl.get())
    property("liveOpsPublicKeyBase64", desktopLiveOpsPublicKey.get())
    property("appVersionCode", "24")
}

val generateDesktopLogoAssets = tasks.register<GenerateDesktopLogoAssets>("generateDesktopLogoAssets") {
    inputLogo.set(appLogoPng)
    outputResources.set(generatedDesktopResources)
    outputIcon.set(generatedWindowsIcon)
}

kotlin {
    jvmToolchain(21)
    jvm()

    sourceSets {
        val jvmMain by getting {
            resources.srcDir(generatedDesktopResources)
            resources.srcDir(generatedDesktopBuildConfig)
            // Consume the canonical game configuration instead of maintaining desktop copies.
            resources.srcDir(rootProject.file("app/src/main/assets/game"))
            // Share the canonical Android changelog with the Windows app bundle.
            resources.srcDir(rootProject.file("app/src/main/assets"))
            dependencies {
                implementation(project(":shared"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.animation)
                implementation(compose.material3)
                // Compose Multiplatform 1.8+ no longer brings this in transitively.
                implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
                // Extended icons (FitScreen, ZoomIn, ZoomOut) are not in the core set.
                implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
                implementation(compose.ui)
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.luaj.jme)
                implementation("net.java.dev.jna:jna:5.17.0")
                implementation("net.java.dev.jna:jna-platform:5.17.0")
                implementation("org.json:json:20250517")
                implementation("ca.weblite:webview:1.12.0")
                val javafxPlatform = when {
                    System.getProperty("os.name").startsWith("Windows", ignoreCase = true) -> "win"
                    System.getProperty("os.name").startsWith("Mac", ignoreCase = true) -> "mac"
                    else -> "linux"
                }
                implementation("org.openjfx:javafx-base:21.0.6:$javafxPlatform")
                implementation("org.openjfx:javafx-graphics:21.0.6:$javafxPlatform")
                implementation("org.openjfx:javafx-controls:21.0.6:$javafxPlatform")
                implementation("org.openjfx:javafx-web:21.0.6:$javafxPlatform")
                implementation("org.openjfx:javafx-swing:21.0.6:$javafxPlatform")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }
    }
}

tasks.matching { it.name == "jvmProcessResources" }.configureEach {
    dependsOn(generateDesktopLogoAssets, generateDesktopBuildConfig)
}
tasks.configureEach {
    if (name.startsWith("package", ignoreCase = true) || name.contains("Distributable", ignoreCase = true)) {
        dependsOn(generateDesktopLogoAssets, generateDesktopBuildConfig)
    }
}

compose.desktop {
    application {
        mainClass = "com.jarrlyyy.guessthenumber.desktop.MainKt"

        nativeDistributions {
            packageName = "GuessTheNumber"
            packageVersion = windowsPackageVersion.get()

            windows {
                iconFile.set(generatedWindowsIcon.get().asFile)
            }
        }
    }
}

// Build a jpackage app-image, then wrap it with Inno Setup. No MSI/WiX target is used.
val packageWindowsInnoSetup = tasks.register<org.gradle.api.tasks.Exec>("packageWindowsInnoSetup") {
    dependsOn("createDistributable", generateDesktopLogoAssets, generateDesktopBuildConfig)
    val appImage = layout.buildDirectory.dir("compose/binaries/main/app/GuessTheNumber")
    val outputDirectory = layout.buildDirectory.dir("compose/binaries/main/exe")
    val installerScript = layout.projectDirectory.file("packageResources/windows/GuessTheNumber.iss")
    inputs.dir(appImage)
    inputs.file(installerScript)
    outputs.dir(outputDirectory)
    doFirst {
        val compiler = providers.environmentVariable("INNO_SETUP_COMPILER")
            .orElse("C:\\Program Files (x86)\\Inno Setup 6\\ISCC.exe").get()
        val compilerFile = file(compiler)
        check(compilerFile.isFile) {
            "Inno Setup compiler was not found at $compiler. Install Inno Setup 6 or set INNO_SETUP_COMPILER."
        }
        val appImageDirectory = appImage.get().asFile
        check(appImageDirectory.isDirectory) {
            "The jpackage app-image was not produced at ${appImageDirectory.absolutePath}."
        }
        outputDirectory.get().asFile.mkdirs()
        commandLine(
            compilerFile.absolutePath,
            "/DAppVersion=${windowsPackageVersion.get()}",
            "/DAppImageDir=${appImageDirectory.absolutePath}",
            "/DAppIcon=${generatedWindowsIcon.get().asFile.absolutePath}",
            "/DOutputDir=${outputDirectory.get().asFile.absolutePath}",
            installerScript.asFile.absolutePath
        )
    }
}

// Show the parity context banner printed by desktop JVM tests in Gradle/CI test logs.
tasks.withType<Test>().matching { it.name == "jvmTest" }.configureEach {
    testLogging {
        showStandardStreams = true
    }
}
