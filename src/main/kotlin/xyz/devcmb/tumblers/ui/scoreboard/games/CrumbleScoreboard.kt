package xyz.devcmb.tumblers.ui.scoreboard.games

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.tumblingPlayer

class CrumbleScoreboard(
    val gameController: GameController,
    val player: Player,
    override val id: String = "crumbleScoreboard"
) : HandledScoreboard {
    override fun getObjectives(scoreboard: Scoreboard): Set<Objective> {
        val activeGame = gameController.activeGame
        if(activeGame !is CrumbleController) return emptySet()

        val objective = scoreboard.registerNewObjective(
            "crumbleGameScoreboard",
            Criteria.create("dummy"),
            Format.mm("<yellow><b>Crumble</b></yellow>")
        )

        objective.displaySlot = DisplaySlot.SIDEBAR

        var rounds = Format.mm("<aqua>Rounds: </aqua>")
        repeat(activeGame.rounds) {
            val result = activeGame.matchResults[it][player.tumblingPlayer.team]
            val append = when(result) {
                CrumbleController.RoundResult.WIN -> "<white>[<green>W</green>]</white>"
                CrumbleController.RoundResult.DRAW -> "<white>[<yellow>D</yellow>]</white>"
                CrumbleController.RoundResult.LOSS -> "<white>[<red>L</red>]</white>"
                null -> "<white>[ ]</white>"
            }
            rounds = rounds.append(Format.mm("${if(it != 0) " " else ""}$append"))
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
            rounds,
            Component.empty(),
            *leaderboard.toTypedArray(),
            Component.empty()
        ))

        return setOf(objective)
    }
}