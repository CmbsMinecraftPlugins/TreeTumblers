package xyz.devcmb.tumblers.commands.event

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.flag.Flag
import dev.rollczi.litecommands.annotations.permission.Permission
import kotlinx.coroutines.launch
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.DatabaseController
import xyz.devcmb.tumblers.controllers.event.EventController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.util.Format
import java.text.SimpleDateFormat

@Command(name = "event")
@Permission("tumbling.event")
class EventCommand {
    @Execute(name = "start")
    fun executeEvent(@Context sender: CommandSender, @Flag("--confirm") confirm: Boolean, @Flag("--finale") finale: Boolean, @Flag("--skip-intro") skipIntro: Boolean) {
        if(EventController.state != EventController.State.EVENT_INACTIVE) {
            sender.sendMessage(Format.error("The event is already active!"))
            return
        }

        if(!confirm) {
            var ready = true
            Team.entries.forEach {
                if(it.getOnlinePlayers().size != it.getAllPlayers().size) {
                    sender.sendMessage(Format.mm("<yellow><team:${it.name}:name> have offline players!</yellow>"))
                    ready = false
                }
            }

            if(!ready) {
                sender.sendMessage(Format.warning("Not all teams have all their players! Re-run with --confirm to execute."))
                return
            }
        }

        EventController.startEvent(finale, skipIntro)
        sender.sendMessage(Format.success("Start signal sent successfully!"))
    }

    @Execute(name = "readycheck")
    fun executeReadyCheck(@Context sender: CommandSender) {
        TreeTumblers.pluginScope.launch {
            sender.sendMessage(Format.success("Ready check sent successfully!"))
            val success = EventController.readyCheck()
            sender.sendMessage(Format.success(Format.mm("Ready check ended with status <b>${if(success) "<green>Success</green>" else "<red>Failure</red>"}</b>")))
        }
    }

    @Execute(name = "timer pause")
    fun executeTimerPause(@Context sender: CommandSender) {
        if(EventController.eventTimer == null) {
            sender.sendMessage(Format.warning("There is no active event timer!"))
            return
        }

        EventController.eventTimer!!.paused = true
        sender.sendMessage(Format.success("Event timer paused successfully!"))
    }

    @Execute(name = "timer unpause")
    fun executeTimerUnpause(@Context sender: CommandSender) {
        if(EventController.eventTimer == null) {
            sender.sendMessage(Format.warning("There is no active event timer!"))
            return
        }

        EventController.eventTimer!!.paused = false
        sender.sendMessage(Format.success("Event timer unpaused successfully!"))
    }

    @Execute(name = "timer set")
    fun executeTimerSet(@Context sender: CommandSender, @Arg time: Int) {
        if(EventController.eventTimer == null) {
            sender.sendMessage(Format.warning("There is no active event timer!"))
            return
        }

        EventController.eventTimer!!.currentTime = time
        sender.sendMessage(Format.success("Event timer set successfully!"))
    }

    @Execute(name = "podiums refresh")
    fun executePodiumsRefresh(@Context sender: CommandSender) {
        EventController.refreshLeaderboards()
        sender.sendMessage(Format.success("Podiums refreshed successfully!"))
    }

    @Execute(name = "recovery list")
    fun executeRecover(@Context sender: CommandSender) {
        var component = Format.mm("<green>Here's a list of recovery states for the event:</green>")
        DatabaseController.recoveryStates.forEachIndexed { index, state ->
            component = component.append(Format.mm("<br><white><yellow><click:run_command:/event recovery state ${state.id}>[${state.id}]</click></yellow> - ${SimpleDateFormat("hh:mm:ss EEE MMM d").format(state.timestamp.time)}${if(index == 0) " <gold>(latest)</gold>" else ""}</white>"))
        }

        sender.sendMessage(component)
    }

    @Execute(name = "recovery state")
    fun executeRecoveryState(@Context sender: CommandSender, @Arg state: DatabaseController.EventRecoveryState) {
        val eventState = state.eventState
        sender.sendMessage(Format.mm(
            "<white><green>Recovery state <yellow>${state.id}</yellow></green><br>" +
                    "Event Active: <aqua>${eventState.eventActive}</aqua><br>" +
                    "Current Game: <aqua>${eventState.currentGame}</aqua><br>" +
                    "Voting Quadrant Games: <aqua>${eventState.votingQuadrantGames}</aqua><br>" +
                    "Played Games: <aqua>${eventState.playedGames}</aqua><br>" +
                    "Last Game Team Placements: <aqua>${eventState.lastGameTeamPlacements?.let { "[${it.size} entries]" } ?: "null"}</aqua><br>" +
                    "Last Game Player Placements: <aqua>${eventState.lastGamePlayerPlacements?.let { "[${it.size} entries]" } ?: "null"}</aqua><br>" +
                    "Last Game Team Scores: <aqua>${eventState.lastGameTeamScores?.let { "[${it.size} entries]" } ?: "null"}</aqua><br>" +
                    "Last Game Player Scores: <aqua>${eventState.lastGamePlayerScores?.let { "[${it.size} entries]" } ?: "null"}</aqua></white>"
        ))
    }

    @Execute(name = "recovery restore")
    fun executeRecoveryRestore(@Context sender: CommandSender, @Arg state: DatabaseController.EventRecoveryState, @Flag("--confirm", "-c") confirm: Boolean) {
        if(!confirm) {
            sender.sendMessage(Format.warning("This action is destructive! Re-run with the --confirm flag to execute."))
            return
        }

        sender.sendMessage(Format.info("Starting restore job"))
        EventController.recover(state)
    }
}
