package xyz.devcmb.font

import xyz.devcmb.util.ConstantPackValues
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath

class DefaultFontWithAscent(val ascent: Int) {
    val providers = listOf(
        FontProvider.SpaceFontProvider(
            mapOf(' ' to 4.0, '\u200C' to 0.0)
        ),
        FontProvider.BitmapFontProvider(
            IdentifiedResource(Namespace.TUMBLING, ResourcePath("font", "nonlatin_european_offset_48.png")),
            56,
            7 - ascent,
            ConstantPackValues.defaultMinecraftNonLatinEuropeanCharacters
        ),
        FontProvider.BitmapFontProvider(
            IdentifiedResource(Namespace.TUMBLING, ResourcePath("font", "accented_offset_48.png")),
            60,
            10 - ascent,
            ConstantPackValues.defaultMinecraftAccentedCharacters
        ),
        FontProvider.BitmapFontProvider(
            IdentifiedResource(Namespace.TUMBLING, ResourcePath("font", "ascii_offset_48.png")),
            56,
            7 - ascent,
            ConstantPackValues.defaultMinecraftAsciiCharacters
        ),
    )
}