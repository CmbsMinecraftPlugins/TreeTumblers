package xyz.devcmb.tumblers.ui.scoreboard.games

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.games.deathrun.DeathrunController
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils

class DeathrunScoreboard(
    val gameController: GameController,
    val player: Player,
    override val id: String = "deathrunScoreboard"
) : HandledScoreboard {
    override fun getObjectives(scoreboard: Scoreboard): Set<Objective> {
        val activeGame = gameController.activeGame
        if(activeGame !is DeathrunController) return emptySet()

        val objective = scoreboard.registerNewObjective(
            "deathrunGameScoreboard",
            Criteria.create("dummy"),
            Format.mm("<yellow><b>Deathrun</b></yellow>")
        )
        objective.displaySlot = DisplaySlot.SIDEBAR

        val rounds = arrayListOf<Component>()
        repeat((activeGame.rounds + 3) / 4) { set ->
            var component = Component.empty()

            repeat(minOf(4, activeGame.rounds - (set * 4))) { setRound ->
                val roundIndex = (set * 4) + setRound
                val result = activeGame.placements[roundIndex][player]

                component = component.append(when (result) {
                    null -> Format.mm("${if(setRound != 0) " " else ""}<white>[ ]</white>")
                    -1 -> Format.mm("${if(setRound != 0) " " else ""}<white>[<yellow>DNF</yellow>]</white>")
                    -2 -> Format.mm("${if(setRound != 0) " " else ""}<white>[<red>\uD83D\uDDE1</red>]</white>")
                    else -> Format.mm("${if (setRound != 0) " " else ""}<white>[<green>$result${MiscUtils.getOrdinalSuffix(result)}</green>]</white>")
                })
            }

            rounds.add(component)
        }

        val leaderboard: ArrayList<Component> = arrayListOf()
        activeGame.getTeamPlacements().forEach { (team, placement) ->
            leaderboard.add(
                Component.empty()
                    .append(Component.text("$placement. ", NamedTextColor.WHITE))
                    .append(team.formattedName)
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text(activeGame.teamScores[team]!!, NamedTextColor.GOLD))
            )
        }

        MiscUtils.addScoreboardObjectiveLines(objective, arrayListOf(
            Component.empty(),
            Format.mm("<aqua>Round <white>${activeGame.currentRound}/${activeGame.rounds}</white></aqua>"),
            *rounds.toTypedArray(),
            Component.empty(),
            *leaderboard.toTypedArray(),
            Component.empty()
        ))

        return setOf(objective)
    }
}