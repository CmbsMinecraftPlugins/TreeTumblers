package xyz.devcmb.tumblers.data

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.event.EventController
import xyz.devcmb.tumblers.controllers.player.PlayerController
import xyz.devcmb.tumblers.util.Font
import xyz.devcmb.tumblers.util.tumblingPlayer

enum class Team(
    val teamName: String,
    val color: TextColor,
    val namedColor: NamedTextColor,
    val concrete: Material,
    val priority: Int,
    val playingTeam: Boolean = true
) {
    RED(
        "Red Raccoons",
        NamedTextColor.RED,
        NamedTextColor.RED,
        Material.RED_CONCRETE,
        1
    ),
    ORANGE(
        "Orange Orcas",
        TextColor.fromHexString("#ff9100")!!,
        NamedTextColor.GOLD,
        Material.ORANGE_CONCRETE,
        2
    ),
    YELLOW(
        "Yellow Yaks",
        NamedTextColor.YELLOW,
        NamedTextColor.YELLOW,
        Material.YELLOW_CONCRETE,
        3
    ),
    GREEN(
        "Green Grasshoppers",
        NamedTextColor.GREEN,
        NamedTextColor.GREEN,
        Material.LIME_CONCRETE,
        4
    ),
    AQUA("Aqua Alpacas",
        NamedTextColor.AQUA,
        NamedTextColor.AQUA,
        Material.LIGHT_BLUE_CONCRETE,
        5
    ),
    BLUE("Blue Boars",
        NamedTextColor.BLUE,
        NamedTextColor.BLUE,
        Material.BLUE_CONCRETE,
        6
    ),
    PURPLE(
        "Purple Pufferfish",
        TextColor.fromHexString("#bb00ff")!!,
        NamedTextColor.DARK_PURPLE,
        Material.PURPLE_CONCRETE,
        7
    ),
    PINK(
        "Pink Parrots",
        TextColor.fromHexString("#ff5cd9")!!,
        NamedTextColor.LIGHT_PURPLE,
        Material.PINK_CONCRETE,
        8
    ),

    // Non-playing teams
    SPECTATORS(
        "Spectators",
        NamedTextColor.GRAY,
        NamedTextColor.GRAY,
        Material.GRAY_CONCRETE,
        9,
        false
    ),
    DEVELOPERS(
        "Developers",
        TextColor.fromHexString("#00c8ff")!!,
        NamedTextColor.AQUA,
        Material.CYAN_CONCRETE,
        10,
        false
    );

    companion object {
        val playingTeams
            get() = Team.entries.filter { it.playingTeam }
        val nonPlayingTeams
            get() = Team.entries.filter { !it.playingTeam }
    }

    val iconGlyph: String = Font.getGlyphString("icon/team/${this.name.lowercase()}")
    val formattedIcon: Component = Font.getGlyph("icon/team/${this.name.lowercase()}")

    val formattedName: Component =
        Component.empty()
            .append(formattedIcon)
            .append(Component.text(" $teamName").color(color))

    val boldedName: Component =
        Component.empty()
            .append(formattedIcon)
            .append(Component.text(" $teamName").color(color).decorate(TextDecoration.BOLD))

    val audience: Audience
        get() {
            return Audience.audience(getOnlinePlayers())
        }

    var score: Int
        get() {
            return EventController.teamScores[this] ?: 0
        }
        set(value) {
            EventController.teamScores.put(this, value)
        }

    fun getOnlinePlayers(): Set<Player> {
        return Bukkit.getOnlinePlayers()
            .filter { it.tumblingPlayer.team == this }
            .toSet()
    }

    fun getAllPlayers(): Set<TumblingPlayer> {
        return PlayerController.players.filter { it.team == this }.toSet()
    }
}