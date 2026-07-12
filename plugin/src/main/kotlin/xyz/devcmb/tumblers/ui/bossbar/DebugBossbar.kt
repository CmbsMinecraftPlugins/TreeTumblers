package xyz.devcmb.tumblers.ui.bossbar

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.Style
import xyz.devcmb.tumblers.Constants
import xyz.devcmb.tumblers.controllers.player.UIController
import xyz.devcmb.tumblers.util.Format

class DebugBossbar : HandledBossbar {
    override val id: String = "debugBossbar"
    override val padding: Int = 0

    override fun getComponent(): Component {
        return UIController.tinsel.draw(250, Style.empty()) { ctx ->
            ctx.drawAligned(Format.mm("<glyph:hud/debug_bg>").shadowColor(ShadowColor.shadowColor(0)), 0.5f)
            ctx.drawAligned(Format.mm(
                "<green>Tree Tumblers</green> <white>|</white> <gold>${Constants.BRANCH}</gold> <gray>(${Constants.VERSION})</gray>"
            ), 0.5f)
        }
    }
}