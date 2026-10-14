plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

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
            // CI supplies a monotonically increasing MSI version for update testing.
            // Local development keeps the existing fallback version.
            packageVersion = providers.environmentVariable("GTN_WINDOWS_PACKAGE_VERSION")
                .orElse("1.0.0")
                .get()

            windows {
                menu = true
                menuGroup = "GuessTheNumber"
                shortcut = true
            }
        }
    }
}
