package xyz.devcmb.tumblers.ui.actionbar.games

import org.bukkit.entity.Player
import xyz.devcmb.fui.draw.TextDrawContext
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.flood_escape.FloodEscapeController
import xyz.devcmb.tumblers.ui.actionbar.HandledActionBar
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.isPlayercheckActive
import kotlin.math.roundToInt

class FloodEscapeActionBar(val player: Player) : HandledActionBar {
    override val id: String = "floodEscapeActionBar"
    override fun draw(ctx: TextDrawContext) {
        if(isPlayercheckActive()) return

        val floodEscape = GameController.activeGame as? FloodEscapeController ?: return
        if(floodEscape.waterTask == null) return

        val distance = floodEscape.currentWaterMovementDirection!!.axisDifference(floodEscape.water!!.location, player.location)
        ctx.drawAligned(Format.mm(
            "<white><aqua>Water Distance:</aqua> ${distance.roundToInt()}</white>"
        ), TextDrawContext.Alignment.CENTER)
    }
}