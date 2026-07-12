package xyz.devcmb.tumblers.controllers.games.sniffer_caretaker

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.base.AbstractGame
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class SnifferCaretakerScoreboard(
    val player: Player,
    gameData: GameData,
) : HandledScoreboard.GameScoreboard(gameData, NamedTextColor.RED) {
    override fun getLines(): ArrayList<Component> {
        val activeGame = GameController.activeGame
        if(activeGame !is SnifferCaretakerController) return arrayListOf()

        val leaderboard: ArrayList<Component> = UserInterfaceUtility.getTeamScoresComponent(player, activeGame)

        return arrayListOf(
            Component.empty(),
            UserInterfaceUtility.timer(activeGame),
            Component.empty(),
            *leaderboard.toTypedArray(),
            Component.empty(),
            UserInterfaceUtility.getIndividualScoreComponent(player, activeGame),
            Component.empty(),
            Format.mm(" <white>Tasks completed: <green>${activeGame.completedTasks[player.tumblingPlayer] ?: 0}</green></white>"),
            Format.mm(" <white>Stars collected: <yellow>${activeGame.starsCollected[player.tumblingPlayer] ?: 0}</yellow></white>"),
            Component.empty()
        )
    }
}