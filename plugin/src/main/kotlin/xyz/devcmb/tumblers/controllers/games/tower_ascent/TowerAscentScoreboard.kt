package xyz.devcmb.tumblers.controllers.games.tower_ascent

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard

class TowerAscentScoreboard(
    val player: Player,
    gameData: GameData
) : HandledScoreboard.GameScoreboard(gameData, NamedTextColor.LIGHT_PURPLE) {
    override fun getLines(): ArrayList<Component> {
        val game = GameController.activeGame as? TowerAscentController ?: return arrayListOf()
        return arrayListOf(
            Component.empty(),
            UserInterfaceUtility.timer(game),
            Component.empty()
        )
    }
}