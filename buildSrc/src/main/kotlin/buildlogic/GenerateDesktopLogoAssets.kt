package buildlogic

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class GenerateDesktopLogoAssets : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputLogo: RegularFileProperty

    @get:OutputDirectory
    abstract val outputResources: DirectoryProperty

    @get:OutputFile
    abstract val outputIcon: RegularFileProperty

    @TaskAction
    fun generate() {
        val sourceFile = inputLogo.get().asFile
        val resourcesDir = outputResources.get().asFile
        resourcesDir.mkdirs()
        sourceFile.copyTo(resourcesDir.resolve("GTN_APP_LOGO.png"), overwrite = true)

        val sourceImage = ImageIO.read(sourceFile)
            ?: throw GradleException("GTN_APP_LOGO.png is not a readable PNG image")
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
                if (!ImageIO.write(resized, "png", output)) {
                    throw GradleException("PNG encoder is unavailable")
                }
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

            writeU16(0)
            writeU16(1)
            writeU16(pngImages.size)

            var imageOffset = 6 + 16 * pngImages.size
            pngImages.forEach { (size, bytes) ->
                write(if (size == 256) 0 else size)
                write(if (size == 256) 0 else size)
                write(0)
                write(0)
                writeU16(1)
                writeU16(32)
                writeU32(bytes.size)
                writeU32(imageOffset)
                imageOffset += bytes.size
            }
            pngImages.forEach { (_, bytes) -> write(bytes) }
        }.toByteArray()

        val iconFile = outputIcon.get().asFile
        iconFile.parentFile.mkdirs()
        iconFile.writeBytes(iconBytes)
    }
}
