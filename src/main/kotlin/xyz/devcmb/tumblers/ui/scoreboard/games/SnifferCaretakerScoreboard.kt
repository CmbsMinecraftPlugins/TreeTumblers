package xyz.devcmb.tumblers.ui.scoreboard.games

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.SnifferCaretakerController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
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

        val leaderboard: ArrayList<Component> = UserInterfaceUtility.getTeamScoresComponent(player, activeGame)

        MiscUtils.addScoreboardObjectiveLines(objective, arrayListOf(
            Component.empty(),
            *leaderboard.toTypedArray(),
            Component.empty(),
            UserInterfaceUtility.getIndividualScoreComponent(player, activeGame),
            Component.empty()
        ))

        return setOf(objective)
    }
}