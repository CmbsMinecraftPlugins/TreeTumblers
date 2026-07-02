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
import kotlin.collections.ArrayList

object ContainersGenerator : FontGenerator {
    override fun generateFonts(builder: ResourcePackBuilder): Iterable<GeneratedFont> {
        val providers: ArrayList<FontProvider.BitmapFontProvider> = ArrayList()
        val containers = javaClass.getResource("/pack/container")
        val file = File(containers!!.toURI().path)

        file.listFiles().filter { it.name.endsWith(".png") || it.isDirectory }.forEachIndexed { index, it ->
            val location = IdentifiedResource(Namespace.TUMBLING, ResourcePath("font", "container", it.name))
            builder.addTexture(
                it,
                location
            )

            val char = (index + 0xF000).toUnicode()
            val (height, ascent) = FontOverrides.getOverrides(it, 256, 13)
            providers.add(FontProvider.BitmapFontProvider(
                location,
                height, ascent,
                listOf(char)
            ))
        }

        return listOf(
            GeneratedFont(IdentifiedResource(Namespace.TUMBLING, ResourcePath("containers")), providers),
        )
    }
}