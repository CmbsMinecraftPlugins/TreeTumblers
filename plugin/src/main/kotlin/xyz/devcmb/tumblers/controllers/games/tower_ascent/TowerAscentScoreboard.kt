package xyz.devcmb.tumblers.controllers.games.tower_ascent

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard

class TowerAscentScoreboard : HandledScoreboard.GameScoreboard(TowerAscentData, NamedTextColor.LIGHT_PURPLE) {
    override val id: String = "towerAscentScoreboard"
    override fun getLines(): ArrayList<Component> {
        return arrayListOf()
    }
}