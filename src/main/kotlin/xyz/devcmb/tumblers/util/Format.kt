package xyz.devcmb.tumblers.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.PlayerController
import xyz.devcmb.tumblers.data.Team

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
}