package xyz.devcmb.tumblers.util

import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.PlayerController
import xyz.devcmb.tumblers.data.TumblingPlayer

fun Player.getTumblingPlayer(): TumblingPlayer? {
    val playerController = ControllerDelegate.getController("playerController") as PlayerController
    return playerController.players.find { it.bukkitPlayer == this }
}