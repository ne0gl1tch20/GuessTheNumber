plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val windowsPackageVersion = providers.environmentVariable("GTN_WINDOWS_PACKAGE_VERSION")
    .orElse("1.0.0")

val generateWindowsBuildVersion by tasks.registering {
    inputs.property("windowsPackageVersion", windowsPackageVersion)
    val outputFile = layout.buildDirectory.file(
        "generated/sources/windowsBuildVersion/com/jarrlyyy/guessthenumber/desktop/update/WindowsBuildVersion.kt"
    )
    outputs.file(outputFile)
    doLast {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            "package com.jarrlyyy.guessthenumber.desktop.update\n\n" +
                "internal const val WINDOWS_BUILD_VERSION = \"" + windowsPackageVersion.get() + "\"\n"
        )
    }
}

kotlin {
    jvmToolchain(17)
    jvm()

    sourceSets {
        val jvmMain by getting {
            kotlin.srcDir(layout.buildDirectory.dir("generated/sources/windowsBuildVersion"))
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
    }
}

tasks.named("compileKotlinJvm").configure {
    dependsOn(generateWindowsBuildVersion)
}

compose.desktop {
    application {
        mainClass = "com.jarrlyyy.guessthenumber.desktop.MainKt"

        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            packageName = "GuessTheNumber"
            packageVersion = windowsPackageVersion.get()

            windows {
                menu = true
                menuGroup = "GuessTheNumber"
                shortcut = true
            }
        }
    }
}
