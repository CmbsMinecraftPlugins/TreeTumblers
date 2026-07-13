package xyz.devcmb.font.generator

import xyz.devcmb.font.FontGenerator
import xyz.devcmb.font.FontProvider
import xyz.devcmb.font.GeneratedFont
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath

object SpacesGenerator : FontGenerator {
    val advances = buildMap {
        put('\uE000', 0.5)
        for (i in 1..11) {
            put('\uE000' + i, (1 shl (i - 1)).toDouble())
        }

        put('\uF000', -0.5)
        for (i in 1..11) {
            put('\uF000' + i, -(1 shl (i - 1)).toDouble())
        }
    }

    override fun generateFonts(builder: ResourcePackBuilder): Iterable<GeneratedFont> {
        return listOf(GeneratedFont(
            IdentifiedResource(Namespace.TUMBLING, ResourcePath("spaces")),
            listOf(FontProvider.SpaceFontProvider(advances))
        ))
    }
}