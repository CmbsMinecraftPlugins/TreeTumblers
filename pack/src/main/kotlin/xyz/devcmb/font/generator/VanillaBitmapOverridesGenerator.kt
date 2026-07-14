package xyz.devcmb.font.generator

import xyz.devcmb.font.FontGenerator
import xyz.devcmb.font.FontProvider
import xyz.devcmb.font.GeneratedFont
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.util.ConstantPackValues
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath

object VanillaBitmapOverridesGenerator : FontGenerator {
    override fun generateFonts(builder: ResourcePackBuilder): Iterable<GeneratedFont> {
        return listOf(
            GeneratedFont(
                IdentifiedResource(Namespace.TUMBLING, ResourcePath("gradient")),
                listOf(
                    FontProvider.SpaceFontProvider(
                        mapOf(' ' to 4.0, '\u200C' to 0.0)
                    ),
                    FontProvider.BitmapFontProvider(
                        IdentifiedResource(Namespace.TUMBLING, ResourcePath("font", "ascii_gradient.png")),
                        8,
                        7,
                        ConstantPackValues.defaultMinecraftAsciiCharacters
                    )
                )
           )
        )
    }
}