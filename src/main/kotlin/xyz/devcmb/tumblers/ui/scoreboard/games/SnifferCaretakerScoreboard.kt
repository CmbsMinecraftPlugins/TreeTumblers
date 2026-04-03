package xyz.devcmb.tumblers.ui.scoreboard.games

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.SnifferCaretakerController
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils

class SnifferCaretakerScoreboard(
    val gameController: GameController,
    val player: Player,
    override val id: String = "snifferCaretakerScoreboard"
) : HandledScoreboard {
    override fun getObjectives(scoreboard: Scoreboard): Set<Objective> {
        val activeGame = gameController.activeGame
        if(activeGame !is SnifferCaretakerController) return emptySet()

        val objective = scoreboard.registerNewObjective(
            "snifferCaretakerScoreboard",
            Criteria.create("dummy"),
            Format.mm("<yellow><b>Sniffer Caretaker</b></yellow>")
        )

        objective.displaySlot = DisplaySlot.SIDEBAR

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
            *leaderboard.toTypedArray()
        ))

        return emptySet()
    }
}