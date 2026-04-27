package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.TimerController
import xyz.devcmb.tumblers.engine.Timer
import xyz.devcmb.tumblers.util.Format

class TimerArgument: ArgumentResolver<CommandSender, Timer>() {
    val timerController by lazy {
        ControllerDelegate.getController("timerController") as TimerController
    }

    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<Timer>,
        argument: String
    ): ParseResult<Timer> {
        val timer = timerController.timers[argument]
        if(timer == null) {
            return ParseResult.failure(Format.error("Timer does not exist!"))
        }

        return ParseResult.success(timer)
    }

    override fun suggest(
        invocation: Invocation<CommandSender>,
        argument: Argument<Timer>,
        context: SuggestionContext
    ): SuggestionResult {
        return timerController.timers.keys.stream().collect(SuggestionResult.collector())
    }
}