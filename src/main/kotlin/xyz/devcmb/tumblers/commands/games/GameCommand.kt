package xyz.devcmb.tumblers.commands.games

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.flag.Flag
import dev.rollczi.litecommands.annotations.join.Join
import dev.rollczi.litecommands.annotations.permission.Permission
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.GameOperatorException
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.engine.DebugToolkit
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@Command(name = "game")
@Permission("tumbling.games")
class GameCommand {
    val gameController: GameController by lazy {
        ControllerDelegate.getController("gameController") as GameController
    }

    @Execute(name = "start")
    fun executeGame(@Context sender: CommandSender, @Arg("game") game: GameController.Game) {
        if(gameController.activeGame != null) {
            sender.sendMessage(Format.error("A game is already active!"))
            return
        }

        try {
            gameController.startGameAsync(game.id)
            sender.sendMessage(Format.success("Started game successfully!"))
        } catch(e: GameOperatorException) {
            sender.sendMessage(Format.error("An error occurred while trying to start the game."))
            DebugUtil.severe("Failed to start game: ${e.message}")
        }
    }

    @Execute(name = "end")
    fun executeEnd(@Context sender: CommandSender, @Flag("--confirm") confirm: Boolean) {
        if(!confirm) {
            sender.sendMessage(Format.warning("This action is destructive! Re-run with --confirm to execute."))
            return
        }

        if(gameController.activeGame == null) {
            sender.sendMessage(Format.error("A game is not active!"))
            return
        }

        if(gameController.activeGameJob == null) {
            sender.sendMessage(Format.error("The game can only be ended after it has started!"))
            return
        }

        gameController.activeGameJob!!.cancel()
        sender.sendMessage(Format.success("Sent signal for game end!"))
    }

    @Execute(name = "event")
    fun executeGameEvent(@Context sender: CommandSender, @Arg("event") event: DebugToolkit.DebuggingEvent) {
        val activeGame = gameController.activeGame
        if(activeGame == null) {
            sender.sendMessage(Format.error("Events can only be executed when a game is active!"))
            return
        }

        val debugToolkit = activeGame.debugToolkit
        if(debugToolkit == null){
            sender.sendMessage(Format.error("Cannot invoke a debug action on a game without a debug toolkit!"))
            return
        }

        try {
            debugToolkit.events[event.name]!!(sender)
            sender.sendMessage(Format.success("Event successfully executed!"))
        } catch(e: Exception) {
            sender.sendMessage(Format.error("An error occurred while trying to execute this event! Check the console for trace"))
            DebugUtil.severe(e.stackTraceToString())
        }
    }

    @Execute(name = "timer")
    fun executeGameTimer(@Context sender: CommandSender, @Arg("value") value: Optional<Int>) {
        val activeGame = gameController.activeGame
        if(activeGame == null) {
            sender.sendMessage(Format.error("Timers can only be retrieved or set when a game is active!"))
            return
        }

        if(activeGame.currentTimer == null) {
            sender.sendMessage(Format.warning("There is no current game timer!"))
            return
        }

        val value = value.getOrNull()
        if(value == null) {
            sender.sendMessage(Format.info("The current game timer is ${activeGame.countdownTime}"))
            return
        }


        activeGame.currentTimer!!.currentTime = value
        sender.sendMessage(Format.success("Timer set successfully!"))
    }

    @Execute(name = "message")
    fun executeMessage(@Context sender: CommandSender, @Join("message") msg: String) {
        val activeGame = gameController.activeGame
        if(activeGame == null) {
            sender.sendMessage(Format.error("Game messages can only be sent if a game is active!"))
            return
        }

        sender.sendMessage(activeGame.gameMessage(Component.text(msg)))
        sender.sendMessage(Format.success("Game message sent successfully!"))
    }
}
