package xyz.devcmb.tumblers.commands.dev

import io.papermc.paper.command.brigadier.CommandSourceStack
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import xyz.devcmb.tumblers.controllers.misc.TimerController
import xyz.devcmb.tumblers.util.Format

@Suppress("unused")
@Permission("tumbling.dev")
class TimerCommand {

    @Command("timer set <timerID> <time>")
    fun executeSet(source: CommandSourceStack, @Argument(suggestions = "timer_id") timerID: String, time: Int) {
        val timer = TimerController.timers[timerID]
        if(timer == null) {
            source.sender.sendMessage(Format.error("A timer with this ID does not exist!"))
            return
        }

        timer.currentTime = time
        source.sender.sendMessage(Format.success("Time set successfully!"))
    }

    @Command("timer pause <timerID>")
    fun executePause(source: CommandSourceStack, @Argument(suggestions = "timer_id") timerID: String) {
        val timer = TimerController.timers[timerID]
        if(timer == null) {
            source.sender.sendMessage(Format.error("A timer with this ID does not exist!"))
            return
        }

        timer.paused = true
        source.sender.sendMessage(Format.success("Timer paused successfully!"))
    }

    @Command("timer unpause <timerID>")
    fun executeUnpause(source: CommandSourceStack, @Argument(suggestions = "timer_id") timerID: String) {
        val timer = TimerController.timers[timerID]
        if(timer == null) {
            source.sender.sendMessage(Format.error("A timer with this ID does not exist!"))
            return
        }

        timer.paused = false
        source.sender.sendMessage(Format.success("Timer unpaused successfully!"))
    }

    @Suggestions("timer_id")
    fun timerIdSuggestions(): List<String> {
        return TimerController.timers.keys.toList()
    }
}
