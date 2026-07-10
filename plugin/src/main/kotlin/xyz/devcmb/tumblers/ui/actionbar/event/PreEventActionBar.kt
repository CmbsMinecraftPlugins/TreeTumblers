package xyz.devcmb.tumblers.ui.actionbar

import me.lucyydotp.tinsel.layout.TextDrawContext
import xyz.devcmb.tumblers.controllers.event.EventController.eventName
import xyz.devcmb.tumblers.controllers.event.EventController.eventTimer
import xyz.devcmb.tumblers.util.Format

class PreEventActionBar : HandledActionBar {
    override val id: String = "preEventActionBar"
    override fun draw(ctx: TextDrawContext) {
        ctx.drawAligned(
            Format.mm("<green><b>Tree Tumblers</b></green> <white>$eventName</white> is starting in <aqua>${eventTimer?.currentTime ?: 0}</aqua> seconds"),
            0.5f
        )
    }
}