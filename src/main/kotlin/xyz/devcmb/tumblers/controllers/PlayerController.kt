package xyz.devcmb.tumblers.controllers

import io.papermc.paper.connection.PlayerLoginConnection
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent
import io.papermc.paper.event.connection.configuration.PlayerConnectionReconfigureEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.devcmb.tumblers.Constants
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.getTumblingPlayer

@Controller("playerController", Controller.Priority.MEDIUM)
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
        val tumblingPlayer = player.getTumblingPlayer()!!

        event.quitMessage(
            Component.text("[").color(NamedTextColor.GRAY)
                .append(Component.text("-").color(NamedTextColor.RED))
                .append(Component.text("] ").color(NamedTextColor.GRAY))
                .append(Format.formatPlayerName(player).color(NamedTextColor.WHITE))
        )

        if(Constants.IS_DEVELOPMENT) {
            DebugUtil.subscribe(player, DebugUtil.DebugLogLevel.WARNING)
            player.sendMessage(
                Component.text("Developer mode is active. You have automatically been subscribed to the warning debug channel.")
                    .color(NamedTextColor.YELLOW)
            )
        }

        val databaseController = ControllerDelegate.getController("databaseController") as DatabaseController
        databaseController.replicatePlayerData(tumblingPlayer)
        players.remove(tumblingPlayer)
    }

    /*
     * This uses an unstable api for preventing a player from connecting in the first place
     * If this ever becomes a problem, either update it to a new, supported method
     * Or just resort to a kick-on-join thing
     */
    @EventHandler
    @Suppress("UnstableApiUsage")
    fun playerLoginEvent(event: PlayerConnectionValidateLoginEvent) {
        val connection = event.connection
        if(connection !is PlayerLoginConnection) return;

        val databaseController = ControllerDelegate.getController("databaseController") as DatabaseController
        val uuid = connection.authenticatedProfile?.id

        if(!databaseController.isWhitelisted(uuid.toString())) {
            event.kickMessage(
                Component.text("----------------------------------", NamedTextColor.RED)
                    .append(Component.newline())
                    .append(Component.text("You are not whitelisted!", NamedTextColor.RED))
                    .append(Component.newline())
                    .append(Component.text("----------------------------------", NamedTextColor.RED))
            )
        }
    }
}