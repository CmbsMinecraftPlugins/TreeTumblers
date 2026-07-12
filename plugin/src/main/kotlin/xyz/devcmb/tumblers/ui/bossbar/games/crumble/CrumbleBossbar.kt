package xyz.devcmb.tumblers.ui.bossbar.games.crumble

import me.lucyydotp.tinsel.layout.TextDrawContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.Style
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.controllers.player.UIController
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.bossbar.HandledBossbar
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Font
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class CrumbleBossbar(
    val player: Player
) : HandledBossbar {
    override val id: String = "crumbleBossbar"
    override val padding: Int = 0

    override fun getComponent(): Component {
        val crumble = GameController.activeGame
            as? CrumbleController
            ?: return DebugUtil.DebugLogLevel.ERROR.icon()
        val matchup = crumble.getCurrentMatchup(player) ?: return Component.empty()

        val friendlyTeam = if(matchup.first == player.tumblingPlayer.team) matchup.first else matchup.second
        val enemyTeam = if(friendlyTeam == matchup.first) matchup.second else matchup.first

        return Component.empty()
            .append(UserInterfaceUtility.negativeSpace(4))
            .append(UIController.tinsel.draw(175, Style.empty()) { ctx ->
                ctx.drawAligned(Font.getGlyph("hud/crumble/matchup_bossbar").shadowColor(ShadowColor.shadowColor(0)), 0.5f)
                ctx.drawAligned(Format.mm("Round ${crumble.currentRound}"), 0.5f)

                ctx.drawAligned(Format.mm(""), 0f)
                ctx.drawWithWidth(friendlyTeam.formattedIcon, 7)
                ctx.moveCursor(ctx.cursorX() + 4, ctx.cursorY())

                displaySide(friendlyTeam.getAllPlayers(), ctx)
                ctx.moveCursor(122, ctx.cursorY())
                displaySide(enemyTeam.getAllPlayers(), ctx)

                ctx.moveCursor(ctx.cursorX() + 2, ctx.cursorY())
                ctx.drawAligned(Format.mm(""), 1f)
                ctx.moveCursor(ctx.cursorX() + 3, ctx.cursorY())
                ctx.draw(enemyTeam.formattedIcon, 1f)
            })
    }

    private fun displaySide(players: Set<TumblingPlayer>, ctx: TextDrawContext) {
        val crumble = GameController.activeGame as CrumbleController
        repeat(4) {
            val enemyPlayer = players.take(4).getOrNull(it) ?: return@repeat

            var component = Format.mm("<head:${enemyPlayer.uuid}>")
            if(enemyPlayer !in crumble.alivePlayers[enemyPlayer.team]!!) {
                component = component.color(NamedTextColor.DARK_GRAY)
            }
            ctx.drawWithWidth(component, 8)
            ctx.moveCursor(ctx.cursorX() + 2, ctx.cursorY())
        }
    }
}