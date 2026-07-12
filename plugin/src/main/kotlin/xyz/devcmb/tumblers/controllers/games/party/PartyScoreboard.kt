package xyz.devcmb.tumblers.controllers.games.party

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class PartyScoreboard(
    val player: Player,
    gameData: GameData,
) : HandledScoreboard.GameScoreboard(gameData, NamedTextColor.AQUA) {
    override fun getLines(): ArrayList<Component> {
        val activeGame = GameController.activeGame
        if(activeGame !is PartyController) return arrayListOf()

        val leaderboard: ArrayList<Component> = UserInterfaceUtility.getTeamScoresComponent(player, activeGame)
        val wins = activeGame.gameOutcomes[player.tumblingPlayer]?.filter { it == PartyController.PartyGameResult.WIN }?.size ?: 0
        val losses = activeGame.gameOutcomes[player.tumblingPlayer]?.filter { it == PartyController.PartyGameResult.LOSS }?.size ?: 0
        val draws = activeGame.gameOutcomes[player.tumblingPlayer]?.filter { it == PartyController.PartyGameResult.DRAW }?.size ?: 0

        return arrayListOf(
            Component.empty(),
            UserInterfaceUtility.timer(activeGame),
            Component.empty(),
            *leaderboard.toTypedArray(),
            Component.empty(),
            UserInterfaceUtility.getIndividualScoreComponent(player, activeGame),
            Component.empty(),
            Format.mm("<white>Wins/Losses/Draws: <green>${wins}</green>/<red>${losses}</red>/<yellow>${draws}</yellow>"),
            Component.empty()
        )
    }
}