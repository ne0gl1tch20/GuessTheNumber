plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val windowsPackageVersion = providers.environmentVariable("GTN_WINDOWS_PACKAGE_VERSION")
    .orElse("1.0.0")

kotlin {
    jvmToolchain(17)
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }
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
                // Preserve the UpgradeCode embedded in the existing Windows MSI.
                upgradeUuid = "6178B622-7859-39F2-8ED0-E0466C715DD1"
                menu = true
                menuGroup = "GuessTheNumber"
                shortcut = true
            }
        }
    }
}
