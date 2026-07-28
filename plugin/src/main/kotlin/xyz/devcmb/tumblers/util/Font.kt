package xyz.devcmb.tumblers.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import xyz.devcmb.tumblers.TreeTumblers
import java.io.File

object Font {
    lateinit var config: YamlConfiguration
    fun loadFontIndex() {
        val indexFile = File(TreeTumblers.plugin.dataPath.toString(), "font_index.yml")
        if(!indexFile.exists()) throw IllegalStateException("Font index file does not exist")

        config = YamlConfiguration.loadConfiguration(indexFile)
    }

    fun getGlyphString(path: String): String {
        val value = findValue(path)
            ?: findValue("icon/missing")
            ?: throw IllegalStateException("Missing icon not included in font resource index")

        return value.second
    }

    fun getGlyph(path: String, shouldColor: Boolean = true): Component {
        val value = findValue(path)
            ?: findValue("icon/missing")
            ?: throw IllegalStateException("Missing icon not included in font resource index")

        var component = Component.empty()
            .append(Component.text(value.second).font(NamespacedKey(TreeTumblers.NAMESPACE, value.first)))
        if(shouldColor) component = component.color(NamedTextColor.WHITE)

        return component
    }

    fun findValue(
        path: String
    ): Pair<String, String>? {
        for (parent in config.getKeys(false)) {
            val section = config.getConfigurationSection(parent) ?: continue

            if (section.contains(path)) {
                return parent to section.getString(path)!!
            }
        }

        return null
    }
}