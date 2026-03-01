package xyz.devcmb.tumblers.controllers

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.util.Format

@Controller("playerController")
class PlayerController : IController {
    val players: ArrayList<TumblingPlayer> = ArrayList()

    override fun init() {
    }

    @EventHandler
    fun playerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // TODO: Replace team with the players team and score with their score from db
        players.add(TumblingPlayer(player, Team.DEVELOPERS, 0))

        event.joinMessage(
            Component.text("[").color(NamedTextColor.GRAY)
                .append(Component.text("+").color(NamedTextColor.GREEN))
                .append(Component.text("] ").color(NamedTextColor.GRAY))
                .append(Format.formatPlayerName(player).color(NamedTextColor.WHITE))
        )
    }

    @EventHandler
    fun playerQuit(event: PlayerQuitEvent) {
        val player = event.player

        event.quitMessage(
            Component.text("[").color(NamedTextColor.GRAY)
                .append(Component.text("-").color(NamedTextColor.RED))
                .append(Component.text("] ").color(NamedTextColor.GRAY))
                .append(Format.formatPlayerName(player).color(NamedTextColor.WHITE))
        )

        // TODO: Replicate db information
        players.remove(players.find { it.player == player })
    }
}