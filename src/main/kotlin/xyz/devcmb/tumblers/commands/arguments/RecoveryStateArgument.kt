package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerRegistry
import xyz.devcmb.tumblers.controllers.DatabaseController
import xyz.devcmb.tumblers.util.Format

class RecoveryStateArgument : ArgumentResolver<CommandSender, DatabaseController.EventRecoveryState>() {
    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<DatabaseController.EventRecoveryState>,
        argument: String
    ): ParseResult<DatabaseController.EventRecoveryState> {
        val recoveryStates = DatabaseController.recoveryStates
        val state = recoveryStates.find { it.id == argument }
        if(state == null) {
            return ParseResult.failure(Format.error("Couldn't find a recovery state with id $argument!"))
        }

        return ParseResult.success(state)
    }

    override fun suggest(
        invocation: Invocation<CommandSender>,
        argument: Argument<DatabaseController.EventRecoveryState>,
        context: SuggestionContext
    ): SuggestionResult {
        return DatabaseController.recoveryStates.map { it.id }.stream().collect(SuggestionResult.collector())
    }
}