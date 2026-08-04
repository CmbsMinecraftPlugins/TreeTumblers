package xyz.devcmb.tumblers.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.util.Format

fun String.validateGame(sender: CommandSender?): GameController.RegisteredGame? {
    val game = GameController.games.find { it.data.id == this }
    if(game == null) {
        sender?.sendMessage(Format.error("A game with the provided ID does not exist!"))
        return null
    }

    return game
}

fun CommandSender.requirePlayer(): Player? {
    if (this !is Player) {
        sendMessage(Format.error("Only players can execute this command!"))
        return null
    }
    return this
}