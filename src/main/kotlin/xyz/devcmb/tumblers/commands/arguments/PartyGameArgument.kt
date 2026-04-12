package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.controllers.games.party.PartyController

class PartyGameArgument: ArgumentResolver<CommandSender, PartyController.PartyGame>() {
    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<PartyController.PartyGame>,
        argument: String
    ): ParseResult<PartyController.PartyGame> {
        val ids = PartyController.teamIds + PartyController.individualIds
        if(argument !in ids) {
            return ParseResult.failure("Invalid party game id!")
        }

        return ParseResult.success(PartyController.PartyGame(argument))
    }

    override fun suggest(
        invocation: Invocation<CommandSender?>?,
        argument: Argument<PartyController.PartyGame?>?,
        context: SuggestionContext?
    ): SuggestionResult? {
        return (PartyController.teamIds + PartyController.individualIds).stream().collect(SuggestionResult.collector())
    }
}