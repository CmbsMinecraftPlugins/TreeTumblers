package xyz.devcmb.tumblers.commands.games

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.GameOperatorException
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format

@Command(name = "game")
@Permission("tumbling.games")
class GameCommand {
    val gameController: GameController by lazy {
        ControllerDelegate.getController("gameController") as GameController
    }

    @Execute(name = "start")
    fun executeGame(@Context sender: CommandSender, @Arg game: GameController.Game) {
        if(gameController.activeGame != null) {
            sender.sendMessage(Format.error("A game is already active!"))
            return
        }

        try {
            gameController.startGame(game.id)
            sender.sendMessage(Format.success("Started game successfully!"))
        } catch(e: GameOperatorException) {
            sender.sendMessage(Format.error("An error occurred while trying to start the game."))
            DebugUtil.severe("Failed to start game: ${e.message}")
        }
    }
}
