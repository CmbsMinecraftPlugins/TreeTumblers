package xyz.devcmb.tumblers.ui.actionbar.games

import me.lucyydotp.tinsel.layout.TextDrawContext
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.brawl.BrawlController
import xyz.devcmb.tumblers.ui.actionbar.HandledActionBar
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class BrawlActionBar(val player: Player) : HandledActionBar {
    override val id: String = "brawlActionBar"
    override fun draw(ctx: TextDrawContext) {
        val brawl = GameController.activeGame as? BrawlController ?: return
        val kit = brawl.playerKits[player.tumblingPlayer] ?: return
        ctx.drawAligned(Format.mm("<glyph:icon/brawl/${kit.name.lowercase()}>").shadowColor(ShadowColor.shadowColor(0)), 0.5f)
    }
}