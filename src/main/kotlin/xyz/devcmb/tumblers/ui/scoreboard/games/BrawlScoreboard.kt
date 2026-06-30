package xyz.devcmb.tumblers.ui.scoreboard.games

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.brawl.BrawlController
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format

class BrawlScoreboard : HandledScoreboard.SidebarScoreboard() {
    override val id: String = "brawlScoreboard"
    override val displayName: String = "<yellow>Brawl</yellow> <dark_gray>|</dark_gray> <gray>Game <game>/<total></gray>"

    override fun getLines(): ArrayList<Component> {
        val activeGame = GameController.activeGame as? BrawlController ?: return arrayListOf()
        return arrayListOf(
            Component.empty(),
            Format.mm(
                "<color:${MiniMessagePlaceholders.Event.EVENT_COLOR}><white>${activeGame.currentTimer?.title ?: "Timer"}:</white> <timer></color>",
                Placeholder.component("timer", activeGame.currentTimer?.format() ?: Component.text("0:00"))
            ),
            Format.mm(
                MiniMessagePlaceholders.Game.SCOREBOARD_CURRENT_ROUND,
                Placeholder.unparsed("current", activeGame.currentRound.toString()),
                Placeholder.unparsed("total", activeGame.rounds.toString())
            ),
            Component.empty()
        )
    }
}