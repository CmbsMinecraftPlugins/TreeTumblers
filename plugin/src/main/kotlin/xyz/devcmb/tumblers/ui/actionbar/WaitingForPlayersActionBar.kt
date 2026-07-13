package xyz.devcmb.tumblers.ui.actionbar

import xyz.devcmb.fui.draw.TextDrawContext
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.util.Format

class WaitingForPlayersActionBar : HandledActionBar {
    override val id: String = "waitingForPlayersActionBar"
    override fun draw(ctx: TextDrawContext) {
        val game = GameController.activeGame ?: return

        ctx.moveCursor(ctx.cursorX, ctx.cursorY + 8)
        ctx.drawAligned(
            Format.mm("<aqua>Waiting for players...</aqua> " +
                "<gray>${game.playerCheckParticipants.filter { entry -> entry.isOnline }.size}/${game.playerCheckParticipants.size}</gray>"),
            TextDrawContext.Alignment.CENTER
        )
    }
}