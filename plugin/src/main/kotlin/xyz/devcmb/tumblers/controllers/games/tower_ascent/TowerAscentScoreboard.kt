package xyz.devcmb.tumblers.controllers.games.tower_ascent

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class TowerAscentScoreboard(
    val player: Player,
    gameData: GameData
) : HandledScoreboard.GameScoreboard(gameData, NamedTextColor.LIGHT_PURPLE) {
    override fun getLines(): ArrayList<Component> {
        val game = GameController.activeGame as? TowerAscentController ?: return arrayListOf()

        val room = game.teamRooms[player.tumblingPlayer.team]
        val lines = arrayListOf(
            Component.empty(),
            UserInterfaceUtility.timer(game),
        )
        room?.let {
            lines.add(Format.mm("<white>Room <color:${MiniMessagePlaceholders.Event.EVENT_COLOR}>${it + 1}/${game.generator.roomCount}</color></white>"))
        }
        lines.add(Component.empty())

        return lines
    }
}