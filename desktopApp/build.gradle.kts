import buildlogic.GenerateDesktopLogoAssets

import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val windowsPackageVersion = providers.environmentVariable("GTN_WINDOWS_PACKAGE_VERSION")
    .orElse("1.0.0")

val appLogoPng = rootProject.file("GTN_APP_LOGO.png")
val generatedDesktopResources = layout.buildDirectory.dir("generated/desktopLogoResources")
val generatedWindowsIcon = layout.buildDirectory.file("generated/windowsIcon/GTN_APP_LOGO.ico")

val generateDesktopLogoAssets = tasks.register<GenerateDesktopLogoAssets>("generateDesktopLogoAssets") {
    inputLogo.set(appLogoPng)
    outputResources.set(generatedDesktopResources)
    outputIcon.set(generatedWindowsIcon)
}

kotlin {
    jvmToolchain(17)
    jvm()

    sourceSets {
        val jvmMain by getting {
            resources.srcDir(generatedDesktopResources)
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
                implementation("net.java.dev.jna:jna:5.17.0")
                implementation("net.java.dev.jna:jna-platform:5.17.0")
                implementation("org.json:json:20250517")
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
    dependsOn(generateDesktopLogoAssets)
}
tasks.configureEach {
    if (name.startsWith("package", ignoreCase = true) || name.contains("Distributable", ignoreCase = true)) {
        dependsOn(generateDesktopLogoAssets)
    }
}

compose.desktop {
    application {
        mainClass = "com.jarrlyyy.guessthenumber.desktop.MainKt"

        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            packageName = "GuessTheNumber"
            packageVersion = windowsPackageVersion.get()

            windows {
                iconFile.set(generatedWindowsIcon.get().asFile)
                // Preserve the UpgradeCode embedded in the existing Windows MSI.
                upgradeUuid = "6178B622-7859-39F2-8ED0-E0466C715DD1"
                menu = true
                menuGroup = "GuessTheNumber"
                shortcut = true
            }
        }
    }
}

// Show the parity context banner printed by desktop JVM tests in Gradle/CI test logs.
tasks.withType<Test>().matching { it.name == "jvmTest" }.configureEach {
    testLogging {
        showStandardStreams = true
    }
}
