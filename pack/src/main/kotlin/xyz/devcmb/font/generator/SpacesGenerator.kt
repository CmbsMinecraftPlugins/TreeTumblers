package xyz.devcmb.font.generator

import xyz.devcmb.font.FontGenerator
import xyz.devcmb.font.FontProvider
import xyz.devcmb.font.GeneratedFont
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath
import xyz.devcmb.util.toUnicode

object SpacesGenerator : FontGenerator {
    val advances = buildMap {
        var advance = -1

        (0..100).forEach {
            put((it + 0xF000).toUnicode(), advance)
            advance = if (advance == -1) -5 else advance - 5
        }

        advance = 1
        (0..100).forEach {
            put((it + 0xE000).toUnicode(), advance)
            advance = if (advance == 1) 5 else advance + 5
        }
    }

    override fun generateFonts(builder: ResourcePackBuilder): Iterable<GeneratedFont> {
        return listOf(GeneratedFont(
            IdentifiedResource(Namespace.TUMBLING, ResourcePath("spaces")),
            listOf(FontProvider.SpaceFontProvider(advances))
        ))
    }
}