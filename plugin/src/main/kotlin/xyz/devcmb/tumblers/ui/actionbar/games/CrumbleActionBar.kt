package xyz.devcmb.tumblers.ui.actionbar.games

import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.entity.Player
import xyz.devcmb.fui.draw.TextDrawContext
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.ui.actionbar.HandledActionBar
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class CrumbleActionBar(val player: Player) : HandledActionBar {
    override val id: String = "crumbleActionBar"
    override fun draw(ctx: TextDrawContext) {
        val crumble = GameController.activeGame as? CrumbleController ?: return
        val kit = crumble.playerKits[player.tumblingPlayer] ?: return
        ctx.drawAligned(
            Format.mm("<glyph:icon/crumble/${kit.id}>").shadowColor(ShadowColor.shadowColor(0)),
            TextDrawContext.Alignment.CENTER
        )
    }
}