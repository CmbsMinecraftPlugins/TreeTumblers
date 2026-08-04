package xyz.devcmb.tumblers.commands.parsers

import io.papermc.paper.command.brigadier.CommandSourceStack
import org.incendo.cloud.bukkit.parser.PlayerParser.PlayerParseException
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.suggestion.BlockingSuggestionProvider
import org.incendo.cloud.suggestion.Suggestion
import xyz.devcmb.tumblers.controllers.player.PlayerController
import xyz.devcmb.tumblers.data.TumblingPlayer

class TumblingPlayerArgumentParser : ArgumentParser<CommandSourceStack, TumblingPlayer>, BlockingSuggestionProvider<CommandSourceStack> {
    override fun parse(
        commandContext: CommandContext<CommandSourceStack>,
        commandInput: CommandInput
    ): ArgumentParseResult<TumblingPlayer> {
        val input = commandInput.readString()

        val player = PlayerController.players.find { it.name == input }
        if (player == null) {
            return ArgumentParseResult.failure(PlayerParseException(input, commandContext))
        }

        return ArgumentParseResult.success(player)
    }

    override fun suggestions(
        context: CommandContext<CommandSourceStack>,
        input: CommandInput
    ): Iterable<Suggestion> {
        return PlayerController.players
            .map { it.name }
            .map(Suggestion::suggestion)
    }
}