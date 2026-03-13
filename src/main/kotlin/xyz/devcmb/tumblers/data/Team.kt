package xyz.devcmb.tumblers.data

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.util.tumblingPlayer

enum class Team(teamName: String, val color: TextColor, val icon: String, val playingTeam: Boolean = true) {
    RED("Red Rabbits", NamedTextColor.RED, "\uE000"),
    ORANGE("Orange Ocelots", TextColor.fromHexString("#ff9100")!!, "\uE001"),
    YELLOW("Yellow Yaks", NamedTextColor.YELLOW, "\uE002"),
    GREEN("Green Grasshoppers", NamedTextColor.GREEN,  "\uE003"),
    AQUA("Aqua Alpacas", NamedTextColor.AQUA, "\uE009"),
    BLUE("Blue Boars", NamedTextColor.BLUE, "\uE004"),
    PURPLE("Purple Pufferfish", TextColor.fromHexString("#bb00ff")!!, "\uE005"),
    PINK("Pink Parrots", TextColor.fromHexString("#ff5cd9")!!, "\uE00A"),

    // Non-playing teams
    SPECTATORS("Spectators", NamedTextColor.WHITE, "\uE007", false),
    DEVELOPERS("Developers", TextColor.fromHexString("#00c8ff")!!, "\uE008", false);

    val FormattedName: Component =
        Component.empty()
            .append(Component.text(icon, NamedTextColor.WHITE).font(NamespacedKey("tumbling", "icons")))
            .append(Component.text(" $teamName").color(color))

    fun getOnlinePlayers(): Set<Player> {
        val players = HashSet<Player>()
        Bukkit.getOnlinePlayers().forEach {
            if(it.tumblingPlayer?.team == this) {
                players.add(it)
            }
        }

        return players
    }
}