package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format

class DebugLogLevelArgument: ArgumentResolver<CommandSender, DebugUtil.DebugLogLevel>() {
    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<DebugUtil.DebugLogLevel>,
        argument: String
    ): ParseResult<DebugUtil.DebugLogLevel> {
        val types = DebugUtil.DebugLogLevel.entries
        val enum = types.find { it.name.lowercase() == argument.lowercase() }

        if(enum == null || enum.name == "none") {
            return ParseResult.failure(Format.error("That isn't a valid debug log level"))
        }

        return ParseResult.success(enum)
    }

    override fun suggest(
        invocation: Invocation<CommandSender?>?,
        argument: Argument<DebugUtil.DebugLogLevel?>?,
        context: SuggestionContext?
    ): SuggestionResult? {
        return DebugUtil.DebugLogLevel.entries
            .stream()
            .map { it.name.lowercase() }
            .filter { it != "none" }
            .collect(SuggestionResult.collector())
    }
}