package xyz.devcmb.tumblers.data

import org.bukkit.entity.Player
import java.util.UUID

data class TumblingPlayer(
    val uuid: UUID,
) {
    var bukkitPlayer: Player? = null
    lateinit var team: Team
    lateinit var name: String
    var score: Int = 0

    val isOnline: Boolean
        get() {
            return bukkitPlayer != null && bukkitPlayer!!.isOnline
        }
}