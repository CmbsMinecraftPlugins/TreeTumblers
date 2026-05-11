package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerRegistry
import xyz.devcmb.tumblers.controllers.player.PlayerController
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.Format

class TumblingPlayerArgument: ArgumentResolver<CommandSender, TumblingPlayer>() {
    val playerController: PlayerController by ControllerRegistry.controller()

    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<TumblingPlayer>,
        argument: String
    ): ParseResult<TumblingPlayer> {
        val players = playerController.players
        val player = players.find { it.name.lowercase() == argument.lowercase() }

        if(player == null) {
            return ParseResult.failure(Format.error("That isn't a valid player"))
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