package xyz.devcmb.tumblers.ui.actionbar.games

import me.lucyydotp.tinsel.layout.TextDrawContext
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.party.PartyController
import xyz.devcmb.tumblers.controllers.games.party.PartyController.PartyGameType
import xyz.devcmb.tumblers.engine.base.AbstractGame
import xyz.devcmb.tumblers.ui.actionbar.HandledActionBar
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.isPlayercheckActive
import xyz.devcmb.tumblers.util.tumblingPlayer

class PartyActionBar(val player: Player) : HandledActionBar {
    override val id: String = "partyActionBar"
    override fun draw(ctx: TextDrawContext) {
        val party = GameController.activeGame as? PartyController ?: return
        if(party.currentState == AbstractGame.State.CUTSCENE || isPlayercheckActive()) return

        val message = when {
            party.currentGameType == PartyGameType.GAME_OVER ->
                Format.mm("<red>Game Over!</red>")

            player in party.disabledGameWaitingPlayers ->
                Format.mm("<yellow>Waiting for <b>team games</b> to activate...</yellow>")

            player in party.waitingTeamPlayers[player.tumblingPlayer.team]!! ->
                Format.mm("<aqua>Waiting for your teammates to finish their games...</aqua>")

            player !in party.inGamePlayers ->
                Format.mm("<aqua>Waiting for a match...</aqua>")

            else -> null
        }

        ctx.drawAligned(message, 0.5f)
    }
}