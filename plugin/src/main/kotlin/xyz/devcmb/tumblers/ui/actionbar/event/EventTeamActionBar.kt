package xyz.devcmb.tumblers.ui.actionbar.event

import org.bukkit.entity.Player
import xyz.devcmb.fui.draw.TextDrawContext
import xyz.devcmb.tumblers.ui.actionbar.HandledActionBar
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class EventTeamActionBar(val player: Player) : HandledActionBar {
    override val id: String = "eventTeamActionBar"
    override fun draw(ctx: TextDrawContext) {
        ctx.drawAligned(
            Format.mm("<glyph:icon/team/${player.tumblingPlayer.team.name.lowercase()}_-15a_16h>"),
            TextDrawContext.Alignment.CENTER.alignmentConstant
        )
    }
}