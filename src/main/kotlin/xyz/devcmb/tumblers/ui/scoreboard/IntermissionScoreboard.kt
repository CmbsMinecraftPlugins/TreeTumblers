package xyz.devcmb.tumblers.ui.scoreboard

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.EventController
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class IntermissionScoreboard(
    val eventController: EventController,
    val player: Player,
    override val id: String = "intermissionScoreboard",
    override val displayName: Component = Format.mm(MiniMessagePlaceholders.Event.EVENT_SCOREBOARD_TITLE)
) : HandledScoreboard.SidebarScoreboard() {
    override fun getLines(): ArrayList<Component> {
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

        val teamComponent: ArrayList<Component> = ArrayList()
        val eventPlacements = eventController.getEventTeamPlacements()
        val team = player.tumblingPlayer.team
        val teamPlacement = eventPlacements.find { it.first == team }

        if(teamPlacement != null) {
            teamComponent.add(
                Format.mm(
                    if(!eventController.scoresHidden) MiniMessagePlaceholders.Game.TEAM_SCOREBOARD_PLACEMENT
                    else MiniMessagePlaceholders.Game.HIDDEN_TEAM_SCOREBOARD_PLACEMENT,
                    Placeholder.unparsed("placement", teamPlacement.second.toString()),
                    Placeholder.component("team", player.tumblingPlayer.team.boldedName),
                    Placeholder.unparsed("score", (eventController.teamScores[team] ?: 0).toString())
                )
            )

            val playerPlacements = eventController.getEventPlayerPlacements()
            val teamPlayers = playerPlacements.filter { it.first.team == team }

            teamPlayers.forEach {
                teamComponent.add(Format.mm(
                    "  " + if(!eventController.scoresHidden) MiniMessagePlaceholders.Game.INDIVIDUAL_SCOREBOARD_PLACEMENT
                    else MiniMessagePlaceholders.Game.HIDDEN_INDIVIDUAL_SCOREBOARD_PLACEMENT,
                    Placeholder.unparsed("placement", it.second.toString()),
                    Placeholder.parsed("head", "<head:${it.first.uuid}>"),
                    Placeholder.component("name", it.first.formattedName),
                    Placeholder.parsed("score", it.first.score.toString())
                ))
            }
        } else {
            teamComponent.add(Format.mm("<gray>You are not participating"))
            teamComponent.add(Format.mm(
                "<icon> <head:${player.uniqueId}> <player>",
                Placeholder.component("icon", team.formattedIcon),
                Placeholder.component("player", Component.text(player.name, team.color))
            ))
        }

        return arrayListOf(
            Component.empty(),
            timer,
            gameComponent,
            Component.empty(),
            *teamComponent.toTypedArray(),
            Component.empty()
        )
    }
}