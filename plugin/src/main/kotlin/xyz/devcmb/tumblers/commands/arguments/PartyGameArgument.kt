package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.controllers.games.party.PartyController
import xyz.devcmb.tumblers.util.Format

class PartyGameArgument: ArgumentResolver<CommandSender, PartyController.PartyGameIdentifier>() {
    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<PartyController.PartyGameIdentifier>,
        argument: String
    ): ParseResult<PartyController.PartyGameIdentifier> {
        if(argument !in PartyController.gameIds) {
            return ParseResult.failure(Format.error("Invalid party game id!"))
        }

        return ParseResult.success(PartyController.PartyGameIdentifier(argument))
    }

    override fun suggest(
        invocation: Invocation<CommandSender?>?,
        argument: Argument<PartyController.PartyGameIdentifier?>?,
        context: SuggestionContext?
    ): SuggestionResult? {
        return PartyController.gameIds.stream().collect(SuggestionResult.collector())
    }
}