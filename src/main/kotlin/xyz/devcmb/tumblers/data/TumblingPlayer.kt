package xyz.devcmb.tumblers.data

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.util.Format
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

    val isOnline: Boolean
        get() {
            return bukkitPlayer != null && bukkitPlayer!!.isOnline
        }

    val formattedName: Component
        get() {
            return Format.formatPlayerName(this)
        }
}