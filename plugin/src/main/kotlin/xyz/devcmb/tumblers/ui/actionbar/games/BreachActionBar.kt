package xyz.devcmb.tumblers.ui.actionbar.games

import org.bukkit.Sound
import org.bukkit.entity.Player
import xyz.devcmb.fui.draw.TextDrawContext
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.breach.BreachController
import xyz.devcmb.tumblers.ui.actionbar.HandledActionBar
import xyz.devcmb.tumblers.util.Format
import kotlin.math.min

class BreachActionBar(val player: Player) : HandledActionBar {
    override val id: String = "breachActionBar"
    override fun draw(ctx: TextDrawContext) {
        val breach = GameController.activeGame as? BreachController ?: return
        val currentTicks = breach.starPickupTimes[player] ?: return
        if(currentTicks == 0 || breach.gameState != BreachController.GameState.GAME_ON) return

        val totalTicks = breach.starPickupTicks.toDouble()
        val progress = min(currentTicks / totalTicks, 1.0)

        var component = Format.mm("<light_purple>Stealing: </light_purple>")

        component = component.append(Format.mm("<white>[</white>"))

        repeat(20) { i ->
            val color = if (progress>= (i.toDouble() / 20.0)) "aqua" else "dark_grey"
            component = component.append(Format.mm("<$color>|</$color>"))
        }
        component = component.append(Format.mm("<white>] ${(progress * 100.0).toInt()}%</white>"))

        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 5f, 0.5f + progress.toFloat())
        ctx.drawAligned(component, TextDrawContext.Alignment.CENTER)
    }
}