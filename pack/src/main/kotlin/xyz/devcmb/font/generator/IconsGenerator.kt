package xyz.devcmb.font.generator

import xyz.devcmb.font.FontGenerator
import xyz.devcmb.font.FontOverrides
import xyz.devcmb.font.FontProvider
import xyz.devcmb.font.GeneratedFont
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath
import xyz.devcmb.util.toUnicode
import java.io.File

object IconsGenerator : FontGenerator {
    override fun generateFonts(builder: ResourcePackBuilder): Iterable<GeneratedFont> {
        val games = javaClass.getResource("/pack/icons")
        val iconsFolder = File(games!!.toURI().path)
        var currentIndex = 0
        val providers = arrayListOf<FontProvider.BitmapFontProvider>()

        fun scanDir(file: File) {
            file.listFiles().filter { it.name.endsWith(".png") || it.isDirectory }.forEach {
                if(it.isDirectory) {
                    scanDir(it)
                    return@forEach
                }

                val name = it.path.toString()
                    .substringAfter(iconsFolder.path.toString() + File.separator)
                    .replace(File.separatorChar, '/')
                val resource = IdentifiedResource(Namespace.TUMBLING, ResourcePath("item", "icon", name))

                val (height, ascent) = FontOverrides.getOverrides(it, 9, 8)
                providers.add(FontProvider.BitmapFontProvider(
                    resource,
                    height, ascent,
                    listOf((currentIndex + 0xF000).toUnicode())
                ))
                currentIndex++
            }
        }
        scanDir(iconsFolder)

        return listOf(GeneratedFont(IdentifiedResource(Namespace.TUMBLING, ResourcePath("icons")), providers))
    }
}