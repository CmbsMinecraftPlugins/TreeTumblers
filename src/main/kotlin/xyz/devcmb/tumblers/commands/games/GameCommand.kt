package xyz.devcmb.tumblers.commands.games

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.GameOperatorException
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.util.DebugUtil

@Command(name = "game")
@Permission("tumbling.games")
class GameCommand {
    val gameController: GameController by lazy {
        ControllerDelegate.getController("gameController") as GameController
    }

    @Execute(name = "start")
    fun executeGame(@Context sender: CommandSender, @Arg game: GameController.Game) {
        if(gameController.activeGame != null) {
            sender.sendMessage(Component.text("A game is already active!", NamedTextColor.RED))
            return
        }

        try {
            gameController.startGame(game.id)
            sender.sendMessage(Component.text("Started game successfully!", NamedTextColor.GREEN))
        } catch(e: GameOperatorException) {
            sender.sendMessage(Component.text("An error occurred while trying to start the game.", NamedTextColor.RED))
            DebugUtil.severe("Failed to start game: ${e.message}")
        }
    }
}
