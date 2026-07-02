package xyz.devcmb.font.generator

import xyz.devcmb.font.FontGenerator
import xyz.devcmb.font.FontProvider
import xyz.devcmb.font.GeneratedFont
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.util.ConstantPackValues
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath

object DefaultAscentGenerator : FontGenerator {
    val ascents = listOf(
        5,
        -5,
        -65,
        -86
    )

    override fun generateFonts(builder: ResourcePackBuilder): Iterable<GeneratedFont> {
        return ascents.map {
            GeneratedFont(IdentifiedResource(Namespace.TUMBLING, ResourcePath("default_shift", "ascent_$it")), listOf(
                FontProvider.BitmapFontProvider(
                    IdentifiedResource(Namespace.MINECRAFT, ResourcePath("font", "ascii.png")),
                    4,
                    it,
                    ConstantPackValues.defaultMinecraftAsciiCharacters
                ),
                FontProvider.ReferenceFontProvider(
                    IdentifiedResource(Namespace.MINECRAFT, ResourcePath("include", "space"))
                )
            ))
        }
    }
}