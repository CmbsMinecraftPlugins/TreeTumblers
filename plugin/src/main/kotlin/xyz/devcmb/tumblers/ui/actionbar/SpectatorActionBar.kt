package xyz.devcmb.tumblers.ui.actionbar

import xyz.devcmb.fui.draw.TextDrawContext
import xyz.devcmb.tumblers.util.Format

class SpectatorActionBar : HandledActionBar {
    override val id: String = "spectatorActionBar"
    override fun draw(ctx: TextDrawContext) {
        ctx.drawAligned(Format.mm("<gray>Spectating</gray>"), TextDrawContext.Alignment.CENTER)
    }
}