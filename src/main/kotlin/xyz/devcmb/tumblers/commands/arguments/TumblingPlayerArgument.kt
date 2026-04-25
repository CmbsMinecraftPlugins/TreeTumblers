package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.PlayerController
import xyz.devcmb.tumblers.data.TumblingPlayer

class TumblingPlayerArgument: ArgumentResolver<CommandSender, TumblingPlayer>() {
    val playerController: PlayerController by lazy {
        ControllerDelegate.getController<PlayerController>()
    }

    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<TumblingPlayer>,
        argument: String
    ): ParseResult<TumblingPlayer> {
        val players = playerController.players
        val player = players.find { it.name.lowercase() == argument.lowercase() }

        if(player == null) {
            return ParseResult.failure("That isn't a valid player")
        }

        return ParseResult.success(player)
    }

    override fun suggest(
        invocation: Invocation<CommandSender?>?,
        argument: Argument<TumblingPlayer?>?,
        context: SuggestionContext?
    ): SuggestionResult? {
        return playerController.players.map { it.name }.stream().collect(SuggestionResult.collector())
    }
}