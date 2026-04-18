package xyz.devcmb.tumblers.ui.scoreboard

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import xyz.devcmb.tumblers.controllers.EventController
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.formattedName
import xyz.devcmb.tumblers.util.tumblingPlayer

class IntermissionScoreboard(
    val eventController: EventController,
    val player: Player,
    override val id: String = "intermissionScoreboard"
) : HandledScoreboard {
    override fun getObjectives(scoreboard: Scoreboard): Set<Objective> {
        val objective = scoreboard.registerNewObjective(
            "intermissionScoreboard",
            Criteria.create("dummy"),
            Format.mm(
                MiniMessagePlaceholders.Event.EVENT_SCOREBOARD_TITLE
            )
        )
        objective.displaySlot = DisplaySlot.SIDEBAR

        val timer: Component = when {
            eventController.readyCheckTimer != null ->
                Format.mm(
                    "<aqua>Ready Check: <white><timer></white></aqua>",
                    Placeholder.component("timer", eventController.eventTimer!!.format())
                )
            eventController.eventTimer != null ->
                Format.mm(
                    "<aqua>${eventController.eventTimerTitle ?: "Timer"}: <white><timer></white></aqua>",
                    Placeholder.component("timer", eventController.eventTimer!!.format())
                )
            eventController.state == EventController.State.EVENT_INACTIVE ->
                Format.mm("<aqua>Event Inactive</aqua>")
            else -> Component.empty()
        }

        val gameComponent = Format.mm(
            MiniMessagePlaceholders.Event.EVENT_SCOREBOARD_GAME,
            Placeholder.unparsed("current", eventController.game.toString()),
            Placeholder.unparsed("max", eventController.totalGames.toString())
        )

        val leaderboard: ArrayList<Component> = ArrayList()
        val placements = eventController.getEventTeamPlacements()
        placements.forEach {
            leaderboard.add(Format.mm(
                MiniMessagePlaceholders.Game.TEAM_SCOREBOARD_PLACEMENT,
                Placeholder.unparsed("placement", it.second.toString()),
                Placeholder.component("team", it.first.formattedName),
                Placeholder.unparsed("score", (eventController.teamScores[it.first] ?: 0).toString())
            ))
        }

        val playerPlacement = eventController.getEventPlayerPlacements().find { it.first.bukkitPlayer == player } ?: throw IllegalStateException("Event player placement not found")
        val playerPlacementComponent = Format.mm(
            MiniMessagePlaceholders.Game.INDIVIDUAL_SCOREBOARD_PLACEMENT,
            Placeholder.unparsed("placement", playerPlacement.second.toString()),
            Placeholder.parsed("head", "<head:${player.uniqueId}>"),
            Placeholder.component("name", player.formattedName),
            Placeholder.parsed("score", player.tumblingPlayer.score.toString())
        )

        MiscUtils.addScoreboardObjectiveLines(objective, arrayListOf(
            Component.empty(),
            timer,
            gameComponent,
            Component.empty(),
            *leaderboard.toTypedArray(),
            Component.empty(),
            playerPlacementComponent,
            Component.empty()
        ))

        return setOf(objective)
    }
}