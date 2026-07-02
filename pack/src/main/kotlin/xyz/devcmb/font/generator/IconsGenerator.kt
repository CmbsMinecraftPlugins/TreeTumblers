package xyz.devcmb.font.generator

import xyz.devcmb.font.FontGenerator
import xyz.devcmb.font.FontProvider
import xyz.devcmb.font.GeneratedFont
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath
import xyz.devcmb.util.dirToBitmapProviders
import java.io.File

object IconsGenerator : FontGenerator {
    override fun generateFonts(builder: ResourcePackBuilder): Iterable<GeneratedFont> {
        val games = javaClass.getResource("/pack/icons")
        val iconsFolder = File(games!!.toURI().path)
        val providers: ArrayList<FontProvider.BitmapFontProvider> = dirToBitmapProviders(builder, iconsFolder)

        return listOf(GeneratedFont(IdentifiedResource(Namespace.TUMBLING, ResourcePath("icons")), providers))
    }
}