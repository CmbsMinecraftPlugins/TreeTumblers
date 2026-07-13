package xyz.devcmb.tumblers.ui.bossbar

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import xyz.devcmb.fui.draw.TextDrawContext
import xyz.devcmb.tumblers.Constants
import xyz.devcmb.tumblers.controllers.player.UIController
import xyz.devcmb.tumblers.util.Format

class DebugBossbar : HandledBossbar {
    override val id: String = "debugBossbar"
    override val padding: Int = 0

    override fun getComponent(): Component {
        return UIController.fUI.draw(250) { ctx ->
            ctx.drawAligned(
                Format.mm("<glyph:hud/debug_bg>").shadowColor(ShadowColor.shadowColor(0)),
                TextDrawContext.Alignment.CENTER
            )
            ctx.drawAligned(
                Format.mm("<green>Tree Tumblers</green> <white>|</white> <gold>${Constants.BRANCH}</gold> <gray>(${Constants.VERSION})</gray>"),
                TextDrawContext.Alignment.CENTER
            )
        }
    }
}