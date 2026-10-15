import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

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

/**
 * Uses the repository's canonical PNG for the runtime window icon and generates a multi-size
 * Windows ICO for the MSI. No checked-in duplicate icon or external converter is required.
 */
val generateDesktopLogoAssets by tasks.registering {
    inputs.file(appLogoPng)
    outputs.dir(generatedDesktopResources)
    outputs.file(generatedWindowsIcon)

    doLast {
        check(appLogoPng.isFile) {
            "Expected the canonical app logo at ${appLogoPng.absolutePath}"
        }

        val resourcesDir = generatedDesktopResources.get().asFile
        resourcesDir.mkdirs()
        appLogoPng.copyTo(resourcesDir.resolve("GTN_APP_LOGO.png"), overwrite = true)

        val sourceImage = ImageIO.read(appLogoPng)
            ?: error("GTN_APP_LOGO.png is not a readable PNG image")
        val sizes = listOf(16, 32, 48, 256)
        val pngImages = sizes.map { size ->
            val resized = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
            val graphics = resized.createGraphics()
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                graphics.drawImage(sourceImage, 0, 0, size, size, null)
            } finally {
                graphics.dispose()
            }
            val bytes = ByteArrayOutputStream().use { output ->
                check(ImageIO.write(resized, "png", output)) { "PNG encoder is unavailable" }
                output.toByteArray()
            }
            size to bytes
        }

        val iconBytes = ByteArrayOutputStream().apply {
            fun writeU16(value: Int) {
                write(value and 0xff)
                write((value ushr 8) and 0xff)
            }
            fun writeU32(value: Int) {
                write(value and 0xff)
                write((value ushr 8) and 0xff)
                write((value ushr 16) and 0xff)
                write((value ushr 24) and 0xff)
            }

            writeU16(0) // Reserved
            writeU16(1) // ICO resource type
            writeU16(pngImages.size)

            var imageOffset = 6 + 16 * pngImages.size
            pngImages.forEach { (size, bytes) ->
                write(if (size == 256) 0 else size)
                write(if (size == 256) 0 else size)
                write(0) // Palette entries
                write(0) // Reserved
                writeU16(1) // Color planes
                writeU16(32) // Bits per pixel
                writeU32(bytes.size)
                writeU32(imageOffset)
                imageOffset += bytes.size
            }
            pngImages.forEach { (_, bytes) -> write(bytes) }
        }.toByteArray()

        val iconFile = generatedWindowsIcon.get().asFile
        iconFile.parentFile.mkdirs()
        iconFile.writeBytes(iconBytes)
    }
}

kotlin {
    jvmToolchain(17)
    jvm()

    sourceSets {
        val jvmMain by getting {
            resources.srcDir(generatedDesktopResources)
            dependencies {
                implementation(project(":shared"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation("net.java.dev.jna:jna:5.17.0")
                implementation("net.java.dev.jna:jna-platform:5.17.0")
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
