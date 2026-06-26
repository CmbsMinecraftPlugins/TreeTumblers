package xyz.devcmb.tumblers.ui.scoreboard.games

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.party.PartyController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class PartyScoreboard(
    val player: Player,
) : HandledScoreboard.SidebarScoreboard() {
    override val displayName: String = "<aqua>Party</aqua> <dark_gray>|</dark_gray> <gray>Game <game>/<total></gray>"
    override val id: String = "partyScoreboard"

    override fun getLines(): ArrayList<Component> {
        val activeGame = GameController.activeGame
        if(activeGame !is PartyController) return arrayListOf()

        val leaderboard: ArrayList<Component> = UserInterfaceUtility.getTeamScoresComponent(player, activeGame)
        return arrayListOf(
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
            Format.mm(" <white>Wins: <green>${activeGame.gameOutcomes[player.tumblingPlayer]?.filter { it == PartyController.PartyGameResult.WIN }?.size ?: 0}</green></white>"),
            Format.mm(" <white>Losses: <red>${activeGame.gameOutcomes[player.tumblingPlayer]?.filter { it == PartyController.PartyGameResult.LOSS }?.size ?: 0}</red></white>"),
            Format.mm(" <white>Draws: <yellow>${activeGame.gameOutcomes[player.tumblingPlayer]?.filter { it == PartyController.PartyGameResult.DRAW }?.size ?: 0}</yellow></white>"),
            Component.empty()
        )
    }
}