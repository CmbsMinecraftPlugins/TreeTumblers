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


    fun success(text: String) = log(text, DebugUtil.DebugLogLevel.SUCCESS)
    fun warning(text: String) = log(text, DebugUtil.DebugLogLevel.WARNING)
    fun error(text: String): Component = log(text, DebugUtil.DebugLogLevel.ERROR)
    fun info(text: String) = log(text, DebugUtil.DebugLogLevel.INFO)

    fun success(text: Component) = log(text, DebugUtil.DebugLogLevel.INFO)
    fun warning(text: Component) = log(text, DebugUtil.DebugLogLevel.WARNING)
    fun error(text: Component) = log(text, DebugUtil.DebugLogLevel.ERROR)
    fun info(text: Component) = log(text, DebugUtil.DebugLogLevel.INFO)

    fun log(text: String, level: DebugUtil.DebugLogLevel) : Component {
        return Component.empty()
            .append(Component.text(level.icon).font(UserInterfaceUtility.WARNINGS))
            .append(Component.text(" $text", level.color))
    }

    fun log(text: Component, level: DebugUtil.DebugLogLevel) : Component {
        return Component.empty()
            .append(Component.text(level.icon).font(UserInterfaceUtility.WARNINGS))
            .append(Component.text(" "))
            .append(text).color(level.color)
    }
}