package xyz.devcmb.tumblers.ui.actionbar.event

import xyz.devcmb.fui.draw.TextDrawContext
import xyz.devcmb.tumblers.controllers.event.EventController.eventName
import xyz.devcmb.tumblers.controllers.event.EventController.eventTimer
import xyz.devcmb.tumblers.ui.actionbar.HandledActionBar
import xyz.devcmb.tumblers.util.Format

class PreEventActionBar : HandledActionBar {
    override val id: String = "preEventActionBar"
    override fun draw(ctx: TextDrawContext) {
        ctx.drawAligned(
            Format.mm("<green><b>Tree Tumblers</b></green> <white>$eventName</white> is starting in <aqua>${eventTimer?.currentTime ?: 0}</aqua> seconds"),
            TextDrawContext.Alignment.CENTER
        )
    }
}