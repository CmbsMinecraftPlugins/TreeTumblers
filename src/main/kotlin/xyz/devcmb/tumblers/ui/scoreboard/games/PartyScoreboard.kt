package xyz.devcmb.tumblers.ui.scoreboard.games

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.games.party.PartyController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils

class PartyScoreboard(
    val gameController: GameController,
    val player: Player,
    override val id: String = "partyScoreboard"
) : HandledScoreboard {
    override fun getObjectives(scoreboard: Scoreboard): Set<Objective> {
        val activeGame = gameController.activeGame
        if(activeGame !is PartyController) return emptySet()

        val objective = scoreboard.registerNewObjective(
            "partyScoreboard",
            Criteria.create("dummy"),
            Format.mm("<yellow><b>Party</b></yellow>")
        )

        objective.displaySlot = DisplaySlot.SIDEBAR

        val leaderboard: ArrayList<Component> = UserInterfaceUtility.getTeamScoresComponent(player, activeGame)
        MiscUtils.addScoreboardObjectiveLines(objective, arrayListOf(
            Component.empty(),
            (
                if(activeGame.teamGamesTimer?.isRunning ?: true)
                    Format.mm(
                        "<white>Team games in: <aqua><time></aqua></white>",
                        Placeholder.component("time", activeGame.teamGamesTimer?.format() ?: Component.text("5:00"))
                    )
                else
                    Format.mm(
                        "<white>Game ends in: <aqua><time></aqua></white>",
                        Placeholder.component("time", activeGame.currentTimer?.format() ?: Component.text("0:00"))
                    )
            ),
            Component.empty(),
            *leaderboard.toTypedArray(),
            Component.empty(),
            UserInterfaceUtility.getIndividualScoreComponent(player, activeGame),
            Component.empty(),
            Format.mm(" <white><green>Wins:</green> ${activeGame.gameOutcomes[player]?.filter { it == PartyController.PartyGameResult.WIN }?.size ?: 0}</white>"),
            Format.mm(" <white><red>Losses:</red> ${activeGame.gameOutcomes[player]?.filter { it == PartyController.PartyGameResult.LOSS }?.size ?: 0}</white>"),
            Format.mm(" <white><yellow>Draws:</yellow> ${activeGame.gameOutcomes[player]?.filter { it == PartyController.PartyGameResult.DRAW }?.size ?: 0}</white>"),
            Component.empty()
        ))

        return setOf(objective)
    }
}