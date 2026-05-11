package xyz.devcmb.tumblers.data

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerRegistry
import xyz.devcmb.tumblers.controllers.event.EventController
import xyz.devcmb.tumblers.controllers.player.PlayerController
import xyz.devcmb.tumblers.util.tumblingPlayer

enum class Team(
    val teamName: String,
    val color: TextColor,
    val namedColor: NamedTextColor,
    val icon: String,
    val priority: Int,
    val playingTeam: Boolean = true
) {
    RED(
        "Red Raccoons",
        NamedTextColor.RED,
        NamedTextColor.RED,
        "\uE000",
        1
    ),
    ORANGE(
        "Orange Orcas",
        TextColor.fromHexString("#ff9100")!!,
        NamedTextColor.GOLD,
        "\uE001",
        2
    ),
    YELLOW(
        "Yellow Yaks",
        NamedTextColor.YELLOW,
        NamedTextColor.YELLOW,
        "\uE002",
        3
    ),
    GREEN(
        "Green Grasshoppers",
        NamedTextColor.GREEN,
        NamedTextColor.GREEN,
        "\uE003",
        4
    ),
    AQUA("Aqua Alpacas",
        NamedTextColor.AQUA,
        NamedTextColor.AQUA,
        "\uE009",
        5
    ),
    BLUE("Blue Boars",
        NamedTextColor.BLUE,
        NamedTextColor.BLUE,
        "\uE004",
        6
    ),
    PURPLE(
        "Purple Pufferfish",
        TextColor.fromHexString("#bb00ff")!!,
        NamedTextColor.DARK_PURPLE,
        "\uE005",
        7
    ),
    PINK(
        "Pink Parrots",
        TextColor.fromHexString("#ff5cd9")!!,
        NamedTextColor.LIGHT_PURPLE,
        "\uE00A",
        8
    ),

    // Non-playing teams
    SPECTATORS(
        "Spectators",
        NamedTextColor.GRAY,
        NamedTextColor.GRAY,
        "\uE007",
        9,
        false
    ),
    DEVELOPERS(
        "Developers",
        TextColor.fromHexString("#00c8ff")!!,
        NamedTextColor.AQUA,
        "\uE008",
        10,
        false
    );

    val formattedIcon: Component =
        Component.text(icon, NamedTextColor.WHITE).font(NamespacedKey("tumbling", "icons"))

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


    private val playerController: PlayerController by ControllerRegistry.controller()
    private val eventController: EventController by ControllerRegistry.controller()

    var score: Int
        get() {
            return eventController.teamScores[this] ?: 0
        }
        set(value) {
            eventController.teamScores.put(this, value)
        }

    fun getOnlinePlayers(): Set<Player> {
        return Bukkit.getOnlinePlayers()
            .filter { it.tumblingPlayer.team == this }
            .toSet()
    }

    fun getAllPlayers(): Set<TumblingPlayer> {
        return playerController.players.filter { it.team == this }.toSet()
    }
}