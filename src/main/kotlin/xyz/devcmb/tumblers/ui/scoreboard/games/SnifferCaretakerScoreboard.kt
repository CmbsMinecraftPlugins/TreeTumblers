package xyz.devcmb.tumblers.ui.scoreboard.games

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.sniffer_caretaker.SnifferCaretakerController
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class SnifferCaretakerScoreboard(
    val player: Player,
) : HandledScoreboard.SidebarScoreboard() {
    override val displayName: String = "<red>Sniffer Caretaker</red> <dark_gray>|</dark_gray> <gray>Game <game>/<total></gray>"
    override val id: String = "snifferCaretakerScoreboard"

    override fun getLines(): ArrayList<Component> {
        val activeGame = GameController.activeGame
        if(activeGame !is SnifferCaretakerController) return arrayListOf()

        val leaderboard: ArrayList<Component> = UserInterfaceUtility.getTeamScoresComponent(player, activeGame)

        return arrayListOf(
            Component.empty(),
            Format.mm(
                "<white>${
                    if(activeGame.currentState == GameBase.State.PREGAME) "Game starts in"
                    else "Game ends in"}: <aqua><timer></aqua></white>",
                Placeholder.component("timer", activeGame.currentTimer?.format() ?: Component.text("10:00"))
            ),
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