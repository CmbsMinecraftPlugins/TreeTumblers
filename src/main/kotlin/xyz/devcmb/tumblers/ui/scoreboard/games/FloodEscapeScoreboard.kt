package xyz.devcmb.tumblers.ui.scoreboard.games

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.flood_escape.FloodEscapeController
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format

class FloodEscapeScoreboard : HandledScoreboard.SidebarScoreboard() {
    override val displayName: String = "<blue>Flood Escape</blue> <dark_gray>|</dark_gray> <gray>Game <game>/<total></gray>"
    override val id: String = "floodEscapeScoreboard"

    override fun getLines(): ArrayList<Component> {
        val activeGame = GameController.activeGame as? FloodEscapeController ?: return arrayListOf()
        return arrayListOf(
            Component.empty(),
            Format.mm(
                MiniMessagePlaceholders.Game.SCOREBOARD_CURRENT_ROUND,
                Placeholder.unparsed("current", activeGame.currentRound.toString()),
                Placeholder.unparsed("total", activeGame.rounds.toString())
            ),
            Component.empty()
        )
    }
}