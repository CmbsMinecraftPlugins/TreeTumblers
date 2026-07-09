package xyz.devcmb.tumblers.ui.scoreboard

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.event.EventController
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class IntermissionScoreboard(
    val player: Player,
) : HandledScoreboard.SidebarScoreboard() {
    override val id: String = "intermission_scoreboard"
    override val displayName: String = MiniMessagePlaceholders.Event.EVENT_SCOREBOARD_TITLE

    override fun getLines(): ArrayList<Component> {
        val timer: Component = Component.empty()
            .append(UserInterfaceUtility.CLOCK)
            .append(when {
                EventController.readyCheckTimer != null ->
                    Format.mm(
                        " <white>Ready Check: <color:${MiniMessagePlaceholders.Event.EVENT_COLOR}><timer></color></white>",
                        Placeholder.component("timer", EventController.readyCheckTimer!!.format())
                    )
                EventController.eventTimer != null ->
                    Format.mm(
                        " <white>${EventController.eventTimer?.title ?: "Timer"}: <color:${MiniMessagePlaceholders.Event.EVENT_COLOR}><timer></color></white>",
                        Placeholder.component("timer", EventController.eventTimer!!.format())
                    )
                EventController.state == EventController.State.EVENT_INACTIVE ->
                    Format.mm(" <white>Event: <color:${MiniMessagePlaceholders.Event.EVENT_COLOR}>Inactive</color></white>")
                else -> Component.empty()
            })

        val gameComponent = Format.mm(
            MiniMessagePlaceholders.Event.EVENT_SCOREBOARD_GAME,
            Placeholder.unparsed("current", EventController.game.toString()),
            Placeholder.unparsed("max", EventController.totalGames.toString())
        )

        val teamComponent: ArrayList<Component> = ArrayList()
        val eventPlacements = EventController.getEventTeamPlacements()
        val team = player.tumblingPlayer.team
        val teamPlacement = eventPlacements.find { it.first == team }

        if(teamPlacement != null) {
            teamComponent.add(
                Format.mm(
                    MiniMessagePlaceholders.Game.TEAM_SCOREBOARD_PLACEMENT,
                    Placeholder.unparsed("placement", if(EventController.scoresHidden) "?" else teamPlacement.second.toString()),
                    Placeholder.component("team", player.tumblingPlayer.team.boldedName),
                    Placeholder.unparsed("score", if(EventController.scoresHidden) "??????" else (EventController.teamScores[team] ?: 0).toString())
                )
            )

            val playerPlacements = EventController.getEventPlayerPlacements()
            val teamPlayers = playerPlacements.filter { it.first.team == team }

            teamPlayers.forEach {
                teamComponent.add(Format.mm(
                    "  " + if(!EventController.scoresHidden) MiniMessagePlaceholders.Game.INDIVIDUAL_SCOREBOARD_PLACEMENT
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
            Format.mm("Score Multiplier: <color:${MiniMessagePlaceholders.Event.EVENT_COLOR}>${EventController.multiplier}x</color>"),
            Component.empty(),
            *teamComponent.toTypedArray(),
            Component.empty()
        )
    }
}