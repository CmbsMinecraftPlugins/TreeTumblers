package xyz.devcmb.font.generator

import xyz.devcmb.font.DefaultFontWithAscent
import xyz.devcmb.font.FontGenerator
import xyz.devcmb.font.GeneratedFont
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath

object DefaultAscentGenerator : FontGenerator {
    val ascents = listOf(
        -4, 4, -8, 8, -12, 12, -16, 16, -20, 20, -24, 24, -28, 28, -32, 32, -36, 36, -40, 40, 44, -44, 48, -48
    )

    override fun generateFonts(builder: ResourcePackBuilder): Iterable<GeneratedFont> {
        return ascents.map {
            GeneratedFont(
                IdentifiedResource(Namespace.TUMBLING, ResourcePath("offset", "default_offset_$it")),
                DefaultFontWithAscent(it).providers
            )
        }
    }
}