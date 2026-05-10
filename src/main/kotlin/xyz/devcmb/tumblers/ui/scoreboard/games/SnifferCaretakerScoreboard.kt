package xyz.devcmb.tumblers.ui.scoreboard.games

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.games.sniffer_caretaker.SnifferCaretakerController
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class SnifferCaretakerScoreboard(
    val gameController: GameController,
    val player: Player,
    override val displayName: Component = Format.mm(
        MiniMessagePlaceholders.Game.SCOREBOARD_TITLE,
        Placeholder.unparsed("name", "Sniffer Caretaker")
    ),
    override val id: String = "snifferCaretakerScoreboard"
) : HandledScoreboard.SidebarScoreboard() {
    override fun getLines(): ArrayList<Component> {
        val activeGame = gameController.activeGame
        if(activeGame !is SnifferCaretakerController) return arrayListOf()

        val leaderboard: ArrayList<Component> = UserInterfaceUtility.getTeamScoresComponent(player, activeGame)

        return arrayListOf(
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