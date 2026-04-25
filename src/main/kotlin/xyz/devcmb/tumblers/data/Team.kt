package xyz.devcmb.tumblers.data

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.EventController
import xyz.devcmb.tumblers.controllers.PlayerController
import xyz.devcmb.tumblers.util.tumblingPlayer

enum class Team(val teamName: String, val color: TextColor, val icon: String, val priority: Int, val playingTeam: Boolean = true) {
    RED("Red Raccoons", NamedTextColor.RED, "\uE000", 1),
    ORANGE("Orange Orcas", TextColor.fromHexString("#ff9100")!!, "\uE001", 2),
    YELLOW("Yellow Yaks", NamedTextColor.YELLOW, "\uE002", 3),
    GREEN("Green Grasshoppers", NamedTextColor.GREEN,  "\uE003", 4),
    AQUA("Aqua Alpacas", NamedTextColor.AQUA, "\uE009", 5),
    BLUE("Blue Boars", NamedTextColor.BLUE, "\uE004", 6),
    PURPLE("Purple Pufferfish", TextColor.fromHexString("#bb00ff")!!, "\uE005", 7),
    PINK("Pink Parrots", TextColor.fromHexString("#ff5cd9")!!, "\uE00A", 8),

    // Non-playing teams
    SPECTATORS("Spectators", NamedTextColor.GRAY, "\uE007", 9, false),
    DEVELOPERS("Developers", TextColor.fromHexString("#00c8ff")!!, "\uE008", 10, false);

    val formattedName: Component =
        Component.empty()
            .append(Component.text(icon, NamedTextColor.WHITE).font(NamespacedKey("tumbling", "icons")))
            .append(Component.text(" $teamName").color(color))

    val audience: Audience
        get() {
            return Audience.audience(getOnlinePlayers())
        }


    private val playerController: PlayerController by lazy {
        ControllerDelegate.getController("playerController") as PlayerController
    }

    private val eventController: EventController by lazy {
        ControllerDelegate.getController<EventController>()
    }

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