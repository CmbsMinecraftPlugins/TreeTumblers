package xyz.devcmb.tumblers.commands.event

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.flag.Flag
import dev.rollczi.litecommands.annotations.permission.Permission
import kotlinx.coroutines.launch
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.EventController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.util.Format

@Command(name = "event")
@Permission("tumbling.event")
class EventCommand {
    val eventController: EventController by lazy {
        ControllerDelegate.getController<EventController>()
    }

    @Execute(name = "start")
    fun executeEvent(@Context sender: CommandSender, @Flag("--confirm") confirm: Boolean, @Flag("--finale") finale: Boolean) {
        if(eventController.state != EventController.State.EVENT_INACTIVE) {
            sender.sendMessage(Format.error("The event is already active!"))
            return
        }

        if(!confirm) {
            var ready = true
            Team.entries.forEach {
                if(it.getOnlinePlayers().size != it.getAllPlayers().size) {
                    sender.sendMessage(Format.mm("<yellow><team> have offline players!</yellow>", Placeholder.component("team", it.formattedName)))
                    ready = false
                }
            }

            if(!ready) {
                sender.sendMessage(Format.warning("Not all teams have all their players! Re-run with --confirm to execute."))
                return
            }
        }


        eventController.startEvent(finale)
        sender.sendMessage(Format.success("Start signal sent successfully!"))
    }

    @Execute(name = "readycheck")
    fun executeReadyCheck(@Context sender: CommandSender) {
        TreeTumblers.pluginScope.launch {
            sender.sendMessage(Format.success("Ready check sent successfully!"))
            val success = eventController.readyCheck()
            sender.sendMessage(Format.success(Format.mm("Ready check ended with status <b>${if(success) "<green>Success</green>" else "<red>Failure</red>"}</b>")))
        }
    }

    @Execute(name = "timer pause")
    fun executeTimerPause(@Context sender: CommandSender) {
        if(eventController.eventTimer == null) {
            sender.sendMessage(Format.warning("There is no active event timer!"))
            return
        }

        eventController.eventTimer!!.paused = true
        sender.sendMessage(Format.success("Event timer paused successfully!"))
    }

    @Execute(name = "timer unpause")
    fun executeTimerUnpause(@Context sender: CommandSender) {
        if(eventController.eventTimer == null) {
            sender.sendMessage(Format.warning("There is no active event timer!"))
            return
        }

        eventController.eventTimer!!.paused = false
        sender.sendMessage(Format.success("Event timer unpaused successfully!"))
    }

    @Execute(name = "timer set")
    fun executeTimerSet(@Context sender: CommandSender, @Arg time: Int) {
        if(eventController.eventTimer == null) {
            sender.sendMessage(Format.warning("There is no active event timer!"))
            return
        }

        eventController.eventTimer!!.currentTime = time
        sender.sendMessage(Format.success("Event timer set successfully!"))
    }
}
