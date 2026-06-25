package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerRegistry
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.util.Format

class GameArgument: ArgumentResolver<CommandSender, GameController.RegisteredGame>() {
    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<GameController.RegisteredGame>,
        argument: String
    ): ParseResult<GameController.RegisteredGame> {
        val game = GameController.games.find { it.id == argument }

        if(game == null) {
            return ParseResult.failure(Format.error("That isn't a valid game"))
        }

        return ParseResult.success(game)
    }

    override fun suggest(
        invocation: Invocation<CommandSender>,
        argument: Argument<GameController.RegisteredGame>,
        context: SuggestionContext
    ): SuggestionResult? {
        return GameController.games
            .stream()
            .map { it.id }
            .collect(SuggestionResult.collector())
    }
}