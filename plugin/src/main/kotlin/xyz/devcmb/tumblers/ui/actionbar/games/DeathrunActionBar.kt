package xyz.devcmb.tumblers.ui.actionbar.games

import me.lucyydotp.tinsel.layout.TextDrawContext
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.deathrun.DeathrunController
import xyz.devcmb.tumblers.ui.actionbar.HandledActionBar
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.isPlayercheckActive
import xyz.devcmb.tumblers.util.formatMsTime
import xyz.devcmb.tumblers.util.tumblingPlayer

class DeathrunActionBar(val player: Player) : HandledActionBar {
    override val id: String = "deathrunActionBar"
    override fun draw(ctx: TextDrawContext) {
        if(!isPlayercheckActive()) return

        val deathrun = GameController.activeGame as? DeathrunController ?: return
        val time = deathrun.completionTimes.getOrElse(player.tumblingPlayer) { deathrun.ticksElapsed }
        val text = formatMsTime(time * 50L)

        // FIXME: deatrun_time_bg just doesn't have proper negative spacing when used here
        ctx.moveCursor(ctx.cursorX(), ctx.cursorY() + 8)
        ctx.drawAligned(Format.mm("<glyph:icon/timer_0a_9h> $text"), 0.5f)
    }
}