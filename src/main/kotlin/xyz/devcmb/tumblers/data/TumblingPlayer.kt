package xyz.devcmb.tumblers.data

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerRegistry
import xyz.devcmb.tumblers.controllers.event.BadgeController
import xyz.devcmb.tumblers.controllers.player.PlayerEventController
import xyz.devcmb.tumblers.util.Format
import java.sql.Timestamp
import java.util.UUID

data class TumblingPlayer(
    val uuid: UUID,
) {
    var bukkitPlayer: Player? = null

    // Values get updated on creation
    // its done this way so changing the score doesn't change the object signature
    // (it did that when the score was in the constructor)
    var team: Team = Team.SPECTATORS
    var name: String = "Player"
    var score: Int = 0
    val badges: HashMap<BadgeController.Badge, Timestamp> = HashMap()

    val isOnline: Boolean
        get() {
            return bukkitPlayer != null && bukkitPlayer!!.isOnline
        }

    val formattedName: Component
        get() {
            return Format.formatPlayerName(this)
        }

    private val playerEventController: PlayerEventController by ControllerRegistry.controller()
    fun showKill(player: TumblingPlayer, score: Int?) {
        playerEventController.addEvent(this, PlayerEventController.Event.KillEvent(this, player, score))
    }
}