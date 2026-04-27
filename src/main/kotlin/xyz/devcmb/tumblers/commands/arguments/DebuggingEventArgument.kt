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
import xyz.devcmb.tumblers.engine.DebugToolkit
import xyz.devcmb.tumblers.util.Format

class DebuggingEventArgument: ArgumentResolver<CommandSender, DebugToolkit.DebuggingEvent>() {
    val gameController: GameController by lazy {
        ControllerDelegate.getController("gameController") as GameController
    }

    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<DebugToolkit.DebuggingEvent>,
        argument: String
    ): ParseResult<DebugToolkit.DebuggingEvent> {
        val events = gameController.activeGame?.debugToolkit?.events?.keys
            ?: return ParseResult.failure(Format.error("Cannot parse a debug event without an active game with a debug toolkit!"))

        if(!events.contains(argument)) {
            return ParseResult.failure(Format.error("That's not a valid event for ${gameController.activeGame!!.id}!"))
        }

        return ParseResult.success(DebugToolkit.DebuggingEvent(argument))
    }

    override fun suggest(
        invocation: Invocation<CommandSender>,
        argument: Argument<DebugToolkit.DebuggingEvent>,
        context: SuggestionContext
    ): SuggestionResult {
        val keys = gameController.activeGame?.debugToolkit?.events?.keys ?: emptySet()
        return keys.stream().collect(SuggestionResult.collector())
    }
}