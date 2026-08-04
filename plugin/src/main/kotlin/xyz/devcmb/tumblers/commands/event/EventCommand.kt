package xyz.devcmb.tumblers.commands.event

import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.launch
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.DatabaseController
import xyz.devcmb.tumblers.controllers.event.EventController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.util.Format
import java.text.SimpleDateFormat

@Suppress("unused")
@Permission("tumbling.event")
class EventCommand {
    @Command("event start")
    fun executeEvent(
        source: CommandSourceStack,
        @Flag("confirm") confirm: Boolean,
        @Flag("finale") finale: Boolean,
        @Flag("skip-intro") skipIntro: Boolean
    ) {
        val sender = source.sender
        if(EventController.state != EventController.State.EVENT_INACTIVE) {
            sender.sendMessage(Format.error("The event is already active!"))
            return
        }

        if(!confirm) {
            var ready = true
            Team.playingTeams.forEach {
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

    @Command("event readycheck")
    fun executeReadyCheck(source: CommandSourceStack) {
        val sender = source.sender
        TreeTumblers.pluginScope.launch {
            sender.sendMessage(Format.success("Ready check sent successfully!"))
            val success = EventController.readyCheck()
            sender.sendMessage(Format.mm("<yellow>Ready check ended with status <b>${if(success) "<green>Success</green>" else "<red>Failure</red>"}</b></yellow>"))
        }
    }

    @Command("event timer pause")
    fun executeTimerPause(source: CommandSourceStack) {
        val sender = source.sender
        if(EventController.eventTimer == null) {
            sender.sendMessage(Format.warning("There is no active event timer!"))
            return
        }

        EventController.eventTimer!!.paused = true
        sender.sendMessage(Format.success("Event timer paused successfully!"))
    }

    @Command("event timer unpause")
    fun executeTimerUnpause(source: CommandSourceStack) {
        val sender = source.sender
        if(EventController.eventTimer == null) {
            sender.sendMessage(Format.warning("There is no active event timer!"))
            return
        }

        EventController.eventTimer!!.paused = false
        sender.sendMessage(Format.success("Event timer unpaused successfully!"))
    }

    @Command("event timer set <time>")
    fun executeTimerSet(source: CommandSourceStack, time: Int) {
        val sender = source.sender
        if(EventController.eventTimer == null) {
            sender.sendMessage(Format.warning("There is no active event timer!"))
            return
        }

        EventController.eventTimer!!.currentTime = time
        sender.sendMessage(Format.success("Event timer set successfully!"))
    }

    @Command("event podiums refresh")
    fun executePodiumsRefresh(source: CommandSourceStack) {
        val sender = source.sender

        EventController.refreshLeaderboards()
        sender.sendMessage(Format.success("Podiums refreshed successfully!"))
    }

    @Command("event recovery list")
    fun executeRecover(source: CommandSourceStack) {
        val sender = source.sender
        var component = Format.mm("<green>Here's a list of recovery states for the event:</green>")
        DatabaseController.recoveryStates.forEachIndexed { index, state ->
            component = component.append(Format.mm(
                "<br><white>" +
                        "<yellow><click:run_command:/event recovery state ${state.id}>[${state.id}]</click></yellow>" +
                        " - ${SimpleDateFormat("hh:mm:ss EEE MMM d").format(state.timestamp.time)}" +
                        "${if(index == DatabaseController.recoveryStates.size - 1) " <gold>(latest)</gold>" else ""}</white>"
            ))
        }

        sender.sendMessage(component)
    }

    @Command("event recovery state <stateId>")
    fun executeRecoveryState(source: CommandSourceStack, stateId: String) {
        val sender = source.sender
        val state = DatabaseController.recoveryStates.find { it.id == stateId }
        if(state == null) {
            sender.sendMessage(Format.mm("Recovery state with provided ID could not be found!"))
            return
        }

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

    @Command("event recovery restore <stateId>")
    fun executeRecoveryRestore(
        source: CommandSourceStack,
        stateId: String,
        @Flag("confirm") confirm: Boolean
    ) {
        val sender = source.sender
        val state = DatabaseController.recoveryStates.find { it.id == stateId }
        if(state == null) {
            sender.sendMessage(Format.mm("Recovery state with provided ID could not be found!"))
            return
        }

        if(!confirm) {
            sender.sendMessage(Format.warning("This action is destructive! Re-run with the --confirm flag to execute."))
            return
        }

        sender.sendMessage(Format.info("Starting restore job"))
        EventController.recover(state)
    }
}
