package xyz.devcmb.font.generator

import xyz.devcmb.font.FontGenerator
import xyz.devcmb.font.FontProvider
import xyz.devcmb.font.GeneratedFont
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath
import xyz.devcmb.util.listResourcesRobust
import java.io.File
import kotlin.collections.ArrayList

object ContainersGenerator : FontGenerator {
    val id = IdentifiedResource(Namespace.TUMBLING, ResourcePath("containers"))

    override fun generateFonts(builder: ResourcePackBuilder): Iterable<GeneratedFont> {
        val providers: ArrayList<FontProvider.BitmapFontProvider> = ArrayList()
        val containers = javaClass.getResource("/pack/container")
        val file = File(containers!!.toURI().path)

        file.listFiles().forEachIndexed { index, it ->
            builder.addTexture(
                it,
                IdentifiedResource(Namespace.TUMBLING, ResourcePath("font", "container"))
            )

            val char = String(Character.toChars(index + 0xF000))
            val file = IdentifiedResource(Namespace.TUMBLING, ResourcePath("font", "container", it.name))
            providers.add(FontProvider.BitmapFontProvider(
                file,
                256, 13,
                listOf(char)
            ))
        }

        return listOf(
            GeneratedFont(IdentifiedResource(Namespace.TUMBLING, ResourcePath("containers")), providers),
        )
    }
}