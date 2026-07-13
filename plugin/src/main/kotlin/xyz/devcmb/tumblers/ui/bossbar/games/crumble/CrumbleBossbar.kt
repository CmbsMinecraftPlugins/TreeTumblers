package xyz.devcmb.tumblers.ui.bossbar.games.crumble

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.entity.Player
import xyz.devcmb.fui.draw.TextDrawContext
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
            .append(UserInterfaceUtility.positiveSpace(3))
            .append(UIController.fUI.draw(175) { ctx ->
                ctx.drawAligned(
                    Font.getGlyph("hud/crumble/matchup_bossbar").shadowColor(ShadowColor.shadowColor(0)),
                    TextDrawContext.Alignment.CENTER
                )
                ctx.drawAligned(Format.mm("Round ${crumble.currentRound}"), TextDrawContext.Alignment.CENTER)

                ctx.moveCursor(0, 0)
                ctx.drawWithWidth(friendlyTeam.formattedIcon, 7.0)
                displaySide(friendlyTeam.getAllPlayers(), ctx)
                ctx.moveCursor(109, 0)
                displaySide(enemyTeam.getAllPlayers(), ctx)
                ctx.moveCursor(ctx.cursorX + 7, 0)
                ctx.draw(enemyTeam.formattedIcon, TextDrawContext.Alignment.RIGHT)
            })
    }

    private fun displaySide(players: Set<TumblingPlayer>, ctx: TextDrawContext) {
        val crumble = GameController.activeGame as CrumbleController
        var sideComponent = Component.empty()
        repeat(4) {
            val enemyPlayer = players.take(4).getOrNull(it) ?: return@repeat

            var component = Format.mm("<head:${enemyPlayer.uuid}> ")
            if(enemyPlayer !in crumble.alivePlayers[enemyPlayer.team]!!) {
                component = component.color(NamedTextColor.DARK_GRAY)
            }
            sideComponent = sideComponent.append(component)
            ctx.moveCursor(ctx.cursorX + 1, ctx.cursorY)
        }

        ctx.draw(sideComponent, TextDrawContext.Alignment.LEFT)
    }
}