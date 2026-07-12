package xyz.devcmb.font.generator

import xyz.devcmb.font.FontGenerator
import xyz.devcmb.font.FontOverrides
import xyz.devcmb.font.FontProvider
import xyz.devcmb.font.GeneratedFont
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Logger
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath
import xyz.devcmb.util.toUnicode
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO


object HudAssetGenerator : FontGenerator {
    override fun generateFonts(builder: ResourcePackBuilder): Iterable<GeneratedFont> {
        val games = javaClass.getResource("/pack/hud")
        val hudFolder = File(games!!.toURI().path)
        val providers: ArrayList<FontProvider.BitmapFontProvider> = ArrayList()
        var currentIndex = 0

        fun scanDir(parent: File) {
            parent.listFiles().filter { it.isDirectory || it.name.endsWith(".png") }.forEach {
                if(it.isDirectory) {
                    scanDir(it)
                    return@forEach
                }

                val image: BufferedImage = ImageIO.read(it)
                val name = it.path.toString()
                    .substringAfter(hudFolder.path.toString() + File.separator)
                    .replace(File.separatorChar, '/')
                val resource = IdentifiedResource(Namespace.TUMBLING, ResourcePath("font", "hud", name))
                builder.addTexture(it, resource)

                val overrides: List<Pair<Int, Int>> = FontOverrides.getOverrides(
                    it,
                    image.height,
                    image.height - 1
                )
                overrides.forEach { (height, ascent) ->
                    providers.add(FontProvider.BitmapFontProvider(
                        resource,
                        height, ascent,
                        listOf((currentIndex + 0xF000).toUnicode())
                    ))
                    currentIndex++
                }
            }
        }

        scanDir(hudFolder)

        return listOf(GeneratedFont(IdentifiedResource(Namespace.TUMBLING, ResourcePath("hud")), providers))
    }
}