package xyz.devcmb.tumblers.data

import org.bukkit.entity.Player

data class TumblingPlayer(val bukkitPlayer: Player, val team: Team, var score: Int)