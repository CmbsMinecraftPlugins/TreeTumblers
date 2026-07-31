package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format

class GameMapArgument : ArgumentResolver<CommandSender, Map>() {
    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<Map>,
        argument: String
    ): ParseResult<Map> {
        val game = getGame(invocation) ?: return ParseResult.failure(Format.error("Cannot get game from invocation!"))

        val map = game.data.maps.find { it.id.equals(argument, true) }
        if(map == null) return ParseResult.failure(Format.error("Game does not have a spawn named $argument!"))

        return ParseResult.success(map)
    }

    override fun suggest(
        invocation: Invocation<CommandSender>,
        argument: Argument<Map>,
        context: SuggestionContext
    ): SuggestionResult {
        val game: GameController.RegisteredGame = getGame(invocation)
            ?: return SuggestionResult.empty()

        return game.data.maps
            .map { it.id }
            .stream()
            .collect(SuggestionResult.collector())
    }

    fun getGame(invocation: Invocation<CommandSender>): GameController.RegisteredGame? {
        DebugUtil.info(invocation.arguments().asList().toString())
        val gameArgument = invocation.arguments().asList()[0] ?: return null
        val game = GameController.games.find { it.data.id.equals(gameArgument, true) }
        return game
    }
}