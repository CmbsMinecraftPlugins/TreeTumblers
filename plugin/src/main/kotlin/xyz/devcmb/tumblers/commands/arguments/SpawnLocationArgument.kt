package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.engine.map.SpawnLocation
import xyz.devcmb.tumblers.util.Format

class SpawnLocationArgument : ArgumentResolver<CommandSender, SpawnLocation>() {
    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<SpawnLocation>,
        argument: String
    ): ParseResult<SpawnLocation> {
        val game = getGame(invocation) ?: return ParseResult.failure(Format.error("Cannot get game from invocation!"))
        if(game.data.spawns == null) return ParseResult.failure(Format.error("Game does not have any spawns!"))

        val spawn = game.data.spawns.find { it.name.equals(argument, true) }
        if(spawn == null) return ParseResult.failure(Format.error("Game does not have a spawn named $argument!"))

        return ParseResult.success(spawn)
    }

    override fun suggest(
        invocation: Invocation<CommandSender>,
        argument: Argument<SpawnLocation>,
        context: SuggestionContext
    ): SuggestionResult {
        val game: GameController.RegisteredGame = getGame(invocation)
            ?: return SuggestionResult.empty()

        if(game.data.spawns == null) return SuggestionResult.empty()

        return game.data.spawns
            .map { it.name }
            .stream()
            .collect(SuggestionResult.collector())
    }

    fun getGame(invocation: Invocation<CommandSender>): GameController.RegisteredGame? {
        val gameArgument = invocation.arguments().asList()[2] ?: return null
        val game = GameController.games.find { it.data.id.equals(gameArgument, true) }
        return game
    }
}