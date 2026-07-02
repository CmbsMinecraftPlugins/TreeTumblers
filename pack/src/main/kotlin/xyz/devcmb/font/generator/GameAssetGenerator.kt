package xyz.devcmb.font.generator

import xyz.devcmb.font.FontGenerator
import xyz.devcmb.font.FontProvider
import xyz.devcmb.font.GeneratedFont
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath
import xyz.devcmb.util.toUnicode
import java.io.File

object GameAssetGenerator : FontGenerator {
    override fun generateFonts(builder: ResourcePackBuilder): Iterable<GeneratedFont> {
        val providers: ArrayList<FontProvider.BitmapFontProvider> = ArrayList()
        val games = javaClass.getResource("/pack/games")
        val file = File(games!!.toURI().path)

        file.listFiles().filter { it.name.endsWith(".png") || it.isDirectory }.forEachIndexed { index, gameFolder ->
            if(!gameFolder.isDirectory) return@forEachIndexed

            val icon = File(gameFolder, "icon.png")
            val logo = File(gameFolder, "logo.png")

            if(!icon.exists() || !logo.exists())
                throw IllegalStateException("Game ${gameFolder.name} does not have valid game assets")

            val iconResource = IdentifiedResource(Namespace.TUMBLING, ResourcePath("item", "game", "${gameFolder.name}_icon.png"))
            val logoResource = IdentifiedResource(Namespace.TUMBLING, ResourcePath("font", "game", "${gameFolder.name}_logo.png"))

            builder.addTexture(logo, logoResource)

            // Font icon
            providers.add(FontProvider.BitmapFontProvider(
                iconResource,
                10, 9,
                listOf((index + 0xC000).toUnicode())
            ))

            // Normal logo
            providers.add(FontProvider.BitmapFontProvider(
                logoResource,
                35, 30,
                listOf((index + 0xE000).toUnicode())
            ))

            // Tab list logo
            providers.add(FontProvider.BitmapFontProvider(
                logoResource,
                45, 14,
                listOf((index + 0xF000).toUnicode())
            ))
        }

        return listOf(GeneratedFont(IdentifiedResource(Namespace.TUMBLING, ResourcePath("games")), providers))
    }
}