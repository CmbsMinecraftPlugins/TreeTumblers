package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.util.Format

class GameArgument: ArgumentResolver<CommandSender, GameController.Game>() {
    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<GameController.Game>,
        argument: String
    ): ParseResult<GameController.Game> {
        val gameController = ControllerDelegate.getController("gameController") as GameController
        val game = gameController.games.find { it.id == argument }

        if(game == null) {
            return ParseResult.failure(Format.error("That isn't a valid game"))
        }

        return ParseResult.success(GameController.Game(game.id))
    }

    override fun suggest(
        invocation: Invocation<CommandSender>,
        argument: Argument<GameController.Game>,
        context: SuggestionContext
    ): SuggestionResult? {
        val gameController = ControllerDelegate.getController("gameController") as GameController
        return gameController.games
            .stream()
            .map { it.id }
            .collect(SuggestionResult.collector())
    }
}