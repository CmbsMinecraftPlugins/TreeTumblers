package xyz.devcmb.tumblers.commands.games

import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.entity.Interaction
import org.bukkit.persistence.PersistentDataType
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import xyz.devcmb.tumblers.GameOperatorException
import xyz.devcmb.tumblers.commands.requirePlayer
import xyz.devcmb.tumblers.commands.validateGame
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.engine.base.AbstractGame
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.toCenterXZLocation

@Suppress("unused")
@Permission("tumbling.dev")
class GameCommand {
    @Command("game start <game>")
    fun executeGame(source: CommandSourceStack, game: String) {
        val sender = source.sender
        if(GameController.activeGame != null) {
            sender.sendMessage(Format.error("A game is already active!"))
            return
        }

        val registeredGame = game.validateGame(sender) ?: return

        try {
            GameController.startGameAsync(registeredGame.data.id)
            sender.sendMessage(Format.success("Started game successfully!"))
        } catch(e: GameOperatorException) {
            sender.sendMessage(Format.error("An error occurred while trying to start the game."))
            DebugUtil.severe("Failed to start game: ${e.message}")
        }
    }

    @Command("game end")
    fun executeEnd(source: CommandSourceStack, @Flag("confirm") confirm: Boolean) {
        val sender = source.sender
        if(!confirm) {
            sender.sendMessage(Format.warning("This action is destructive! Re-run with --confirm to execute."))
            return
        }

        if(GameController.activeGame == null) {
            sender.sendMessage(Format.error("A game is not active!"))
            return
        }

        if(GameController.activeGameJob == null) {
            sender.sendMessage(Format.error("The game can only be ended after it has started!"))
            return
        }

        GameController.activeGameJob!!.cancel()
        sender.sendMessage(Format.success("Sent signal for game end!"))
    }

    @Command("game event <event>")
    fun executeGameEvent(source: CommandSourceStack, event: String) {
        val sender = source.sender

        val activeGame = GameController.activeGame
        if(activeGame == null) {
            sender.sendMessage(Format.error("Events can only be executed when a game is active!"))
            return
        }

        val debugToolkit = activeGame.debugToolkit
        if(debugToolkit == null){
            sender.sendMessage(Format.error("Cannot invoke a debug action on a game without a debug toolkit!"))
            return
        }

        val event = debugToolkit.events[event]
        if(event == null) {
            sender.sendMessage(Format.error("An event with the provided name was not found!"))
            return
        }

        try {
            event.invoke(sender)
            sender.sendMessage(Format.success("Event successfully executed!"))
        } catch(e: Exception) {
            sender.sendMessage(Format.error("An error occurred while trying to execute this event! Check the console for trace"))
            DebugUtil.severe(e.stackTraceToString())
        }
    }

    @Command("game timer <value>")
    fun executeGameTimer(source: CommandSourceStack, value: Int) {
        val sender = source.sender
        val activeGame = GameController.activeGame
        if(activeGame == null) {
            sender.sendMessage(Format.error("Timers can only be retrieved or set when a game is active!"))
            return
        }

        if(activeGame.currentTimer == null) {
            sender.sendMessage(Format.warning("There is no current game timer!"))
            return
        }

        activeGame.currentTimer!!.currentTime = value
        sender.sendMessage(Format.success("Timer set successfully!"))
    }

    @Command("game message")
    fun executeMessage(source: CommandSourceStack, @Greedy msg: String) {
        val sender = source.sender
        val activeGame = GameController.activeGame
        if(activeGame == null) {
            sender.sendMessage(Format.error("Game messages can only be sent if a game is active!"))
            return
        }

        sender.sendMessage(activeGame.gameMessage(Component.text(msg)))
        sender.sendMessage(Format.success("Game message sent successfully!"))
    }

    @Command("game spawn summon <gameId> <location>")
    fun executeSpawnSummon(
        source: CommandSourceStack,
        @Argument(suggestions = "registered_games") gameId: String,
        @Argument(suggestions = "spawn_locations") location: String
    ) {
        val sender = source.sender
        val player = source.sender.requirePlayer() ?: return
        val game = gameId.validateGame(sender) ?: return

        if(game.data.spawns == null) {
            sender.sendMessage(Format.error("This game has no spawn locations!"))
            return
        }

        val spawn = game.data.spawns.find { it.name.equals(location, true) }
        if(spawn == null) {
            sender.sendMessage(Format.warning("A spawn location with the provided ID does not exist!"))
            return
        }

        val playerLocation = player.location.toCenterXZLocation()
        playerLocation.world.spawn(playerLocation, Interaction::class.java) {
            it.persistentDataContainer.set(AbstractGame.spawnKey, PersistentDataType.STRING, spawn.name.lowercase())
        }

        player.sendMessage(Format.success(Format.mm("Summoned spawn <white>${spawn.name}</white> successfully!")))
    }

    @Command("game playercheck skip")
    fun playerCheckSkip(source: CommandSourceStack) {
        val sender = source.sender
        val activeGame = GameController.activeGame
        if(activeGame == null) {
            sender.sendMessage(Format.error("No game is currently active!"))
            return
        }

        if(!activeGame.playerCheckActive) {
            sender.sendMessage(Format.error("A player check is not active!"))
            return
        }

        activeGame.playerCheckSkipped = true
        sender.sendMessage(Format.success("Skipped the player check successfully!"))
    }

    @Command("game playercheck permaskip")
    fun playerCheckPermaSkip(source: CommandSourceStack) {
        val sender = source.sender
        val activeGame = GameController.activeGame
        if(activeGame == null) {
            sender.sendMessage(Format.error("No game is currently active!"))
            return
        }

        activeGame.playerCheckPersistentSkipped = true
        sender.sendMessage(Format.success("Persistently skipped the player check successfully!"))
    }

    @Command("game playercheck unpermaskip")
    fun playerCheckUnPermaSkip(source: CommandSourceStack) {
        val sender = source.sender
        val activeGame = GameController.activeGame
        if(activeGame == null) {
            sender.sendMessage(Format.error("No game is currently active!"))
            return
        }

        activeGame.playerCheckPersistentSkipped = false
        sender.sendMessage(Format.success("Un-persistently skipped the player check successfully!"))
    }

    @Suggestions("spawn_locations")
    fun suggestSpawnLocations(context: CommandContext<CommandSourceStack>, input: CommandInput): List<String> {
        val gameId = context.get<String>("gameId")
        val registeredGame = gameId.validateGame(null) ?: return emptyList()

        return registeredGame.data.spawns?.map { it.name } ?: emptyList()
    }
}
