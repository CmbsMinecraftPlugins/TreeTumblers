package xyz.devcmb.tumblers.controllers.games.tower_ascent

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard

class TowerAscentScoreboard(
    val player: Player,
    gameData: GameData
) : HandledScoreboard.GameScoreboard(gameData, NamedTextColor.LIGHT_PURPLE) {
    override fun getLines(): ArrayList<Component> {
        return arrayListOf()
    }
}