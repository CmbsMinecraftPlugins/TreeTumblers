package xyz.devcmb.tumblers.commands.dev

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.engine.Timer
import xyz.devcmb.tumblers.util.Format

@Command(name = "timer")
@Permission("tumbling.dev")
class TimerCommand {

    @Execute(name = "set")
    fun executeSet(@Context sender: CommandSender, @Arg timer: Timer, @Arg time: Int) {
        timer.currentTime = time
        sender.sendMessage(Format.success("Time set successfully!"))
    }

    @Execute(name = "pause")
    fun executePause(@Context sender: CommandSender, @Arg timer: Timer) {
        timer.paused = true
        sender.sendMessage(Format.success("Timer paused successfully!"))
    }

    @Execute(name = "unpause")
    fun executeUnpause(@Context sender: CommandSender, @Arg timer: Timer) {
        timer.paused = false
        sender.sendMessage(Format.success("Timer unpaused successfully!"))
    }
}
