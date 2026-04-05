package xyz.devcmb.tumblers.data

import org.bukkit.entity.Player
import java.util.UUID

data class TumblingPlayer(
    var bukkitPlayer: Player?,
    val uuid: UUID,
    var name: String,
    var team: Team,
    var score: Int
)