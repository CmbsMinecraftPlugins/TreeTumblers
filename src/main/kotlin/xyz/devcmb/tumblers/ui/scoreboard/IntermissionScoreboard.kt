package xyz.devcmb.tumblers.ui.scoreboard

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.EventController
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.formattedName
import xyz.devcmb.tumblers.util.tumblingPlayer

class IntermissionScoreboard(
    val eventController: EventController,
    val player: Player,
    override val id: String = "intermissionScoreboard",
    override val displayName: Component = Format.mm(MiniMessagePlaceholders.Event.EVENT_SCOREBOARD_TITLE)
) : HandledScoreboard.SidebarScoreboard() {
    override fun getLines(): ArrayList<Component> {
        val playerPlacement = eventController.getEventPlayerPlacements()
            .find { it.first.bukkitPlayer == player }

        if(playerPlacement == null) {
            return arrayListOf(
                Component.empty(),
                Format.mm("<aqua>Loading...</aqua>"),
                Component.empty()
            )
        }

        val timer: Component = Component.empty()
            .append(UserInterfaceUtility.CLOCK)
            .append(when {
                eventController.readyCheckTimer != null ->
                    Format.mm(
                        " <color:${MiniMessagePlaceholders.Event.EVENT_COLOR}>Ready Check: <white><timer></white></color>",
                        Placeholder.component("timer", eventController.readyCheckTimer!!.format())
                    )
                eventController.eventTimer != null ->
                    Format.mm(
                        " <color:${MiniMessagePlaceholders.Event.EVENT_COLOR}>${eventController.eventTimerTitle ?: "Timer"}: <white><timer></white></color>",
                        Placeholder.component("timer", eventController.eventTimer!!.format())
                    )
                eventController.state == EventController.State.EVENT_INACTIVE ->
                    Format.mm(" <color:${MiniMessagePlaceholders.Event.EVENT_COLOR}>Event Inactive</color>")
                else -> Component.empty()
            })

        val gameComponent = Format.mm(
            MiniMessagePlaceholders.Event.EVENT_SCOREBOARD_GAME,
            Placeholder.unparsed("current", eventController.game.toString()),
            Placeholder.unparsed("max", eventController.totalGames.toString())
        )

        val leaderboard: ArrayList<Component> = ArrayList()
        val placements = eventController.getEventTeamPlacements()
        placements.forEach {
            leaderboard.add(Format.mm(
                if(!eventController.scoresHidden) MiniMessagePlaceholders.Game.TEAM_SCOREBOARD_PLACEMENT
                else MiniMessagePlaceholders.Game.HIDDEN_TEAM_SCOREBOARD_PLACEMENT,
                Placeholder.unparsed("placement", it.second.toString()),
                Placeholder.component("team", it.first.formattedName),
                Placeholder.unparsed("score", (eventController.teamScores[it.first] ?: 0).toString())
            ))
        }

        val playerPlacementComponent = Format.mm(
            if(!eventController.scoresHidden) MiniMessagePlaceholders.Game.INDIVIDUAL_SCOREBOARD_PLACEMENT
                else MiniMessagePlaceholders.Game.HIDDEN_INDIVIDUAL_SCOREBOARD_PLACEMENT,
            Placeholder.unparsed("placement", playerPlacement.second.toString()),
            Placeholder.parsed("head", "<head:${player.uniqueId}>"),
            Placeholder.component("name", player.formattedName),
            Placeholder.parsed("score", player.tumblingPlayer.score.toString())
        )

        return arrayListOf(
            Component.empty(),
            timer,
            gameComponent,
            Component.empty(),
            *leaderboard.toTypedArray(),
            Component.empty(),
            playerPlacementComponent,
            Component.empty()
        )
    }
}