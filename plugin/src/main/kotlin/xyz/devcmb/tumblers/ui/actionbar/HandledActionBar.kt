package xyz.devcmb.tumblers.ui.actionbar

import me.lucyydotp.tinsel.layout.TextDrawContext

interface HandledActionBar {
    val id: String
    fun draw(ctx: TextDrawContext)
}