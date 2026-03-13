package xyz.devcmb.tumblers.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.ui.UserInterfaceUtility

object Format {
    fun formatPlayerName(player: Player): Component {
        val tumblingPlayer = player.tumblingPlayer
        if (tumblingPlayer == null) {
            return Component.empty()
                .append(
                    Component.text(Team.SPECTATORS.icon, NamedTextColor.WHITE)
                        .font(NamespacedKey("tumbling", "icons"))
                )
                .append(Component.text(" "))
                .append(Component.text(player.name, NamedTextColor.WHITE))
        }

        val team = tumblingPlayer.team
        return Component.empty()
            .append(
                Component.text(team.icon, NamedTextColor.WHITE)
                    .font(NamespacedKey("tumbling", "icons"))
            )
            .append(Component.text(" "))
            .append(Component.text(player.name, team.color))
    }

    fun error(text: String): Component {
        return log(text, DebugUtil.DebugLogLevel.ERROR)
    }

    // consistency sake
    fun severe(text: String) = error(text)

    fun success(text: String): Component {
        return log(text, DebugUtil.DebugLogLevel.SUCCESS)
    }

    fun warning(text: String): Component {
        return log(text, DebugUtil.DebugLogLevel.WARNING)
    }

    fun info(text: String): Component {
        return log(text, DebugUtil.DebugLogLevel.INFO)
    }

    fun log(text: String, level: DebugUtil.DebugLogLevel) : Component {
        return Component.empty()
            .append(Component.text(level.icon).font(UserInterfaceUtility.WARNINGS))
            .append(Component.text(" $text", level.color))
    }
}