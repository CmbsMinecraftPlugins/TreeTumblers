package xyz.devcmb.tumblers.ui.scoreboard.games

import net.kyori.adventure.text.Component
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard

class BrawlScoreboard : HandledScoreboard.SidebarScoreboard() {
    override val id: String = "brawlScoreboard"
    override val displayName: String = "<yellow>Brawl</yellow> <dark_gray>|</dark_gray> <gray>Game <game>/<total></gray>"

    override fun getLines(): ArrayList<Component> {
        return arrayListOf()
    }
}