package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.data.Team

class TeamArgument: ArgumentResolver<CommandSender, Team>() {
    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<Team>,
        argument: String
    ): ParseResult<Team> {
        val types = Team.entries
        val enum = types.find { it.name.lowercase() == argument.lowercase() }

        if(enum == null) {
            return ParseResult.failure("That isn't a valid team")
        }

        return ParseResult.success(enum)
    }

    override fun suggest(
        invocation: Invocation<CommandSender>,
        argument: Argument<Team>,
        context: SuggestionContext
    ): SuggestionResult? {
        return Team.entries
            .stream()
            .map { it.name.lowercase() }
            .collect(SuggestionResult.collector())
    }
}