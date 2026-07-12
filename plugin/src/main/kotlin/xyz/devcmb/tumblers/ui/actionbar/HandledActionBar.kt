package xyz.devcmb.tumblers.ui.actionbar

import me.lucyydotp.tinsel.layout.TextDrawContext

interface HandledActionBar {
    val id: String
    /**
     * Called every tick with a passed in [TextDrawContext]
     *
     * When invoked, the cursor will always be set at the origin position
     **/
    fun draw(ctx: TextDrawContext)

    /** Called when the action bar is enabld **/
    fun enable() {}

    /** Called when the action bar is disabled **/
    fun disable() {}
}