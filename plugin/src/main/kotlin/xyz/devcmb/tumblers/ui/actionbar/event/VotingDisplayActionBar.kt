package xyz.devcmb.tumblers.ui.actionbar

import me.lucyydotp.tinsel.layout.TextDrawContext
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.event.VotingController
import xyz.devcmb.tumblers.util.Format

class VotingDisplayActionBar(val player: Player) : HandledActionBar {
    override val id: String = "votingDisplayActionBar"
    override fun draw(ctx: TextDrawContext) {
        val currentQuadrant: Int = VotingController.playerQuadrants[player] ?: return
        val currentGame = VotingController.quadrantGames[currentQuadrant]!!

        ctx.drawAligned(Format.mm("<glyph:game/${currentGame.data.id}_icon_-12a_18h>"), 0.5f)
    }
}