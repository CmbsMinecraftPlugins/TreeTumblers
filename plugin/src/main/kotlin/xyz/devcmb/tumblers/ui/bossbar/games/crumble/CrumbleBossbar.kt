package xyz.devcmb.tumblers.ui.bossbar.games.crumble

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.bossbar.HandledBossbar
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Font
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.formatToMSS
import xyz.devcmb.tumblers.util.tumblingPlayer
import kotlin.math.roundToInt

class CrumbleBossbar(
    val player: Player
) : HandledBossbar {
    override val id: String = "crumbleBossbar"
    override val padding: Int = 0

    val headLength: Double = 8.0
    val skullLength: Double = 8.5

    override fun getComponent(): Component {
        val crumble = GameController.activeGame
            as? CrumbleController
            ?: return DebugUtil.DebugLogLevel.ERROR.icon()

        var component = Component.empty()
        val matchup = crumble.getCurrentMatchup(player) ?: return Component.empty()

        val friendlyTeam = if(matchup.first == player.tumblingPlayer.team) matchup.first else matchup.second
        val enemyTeam = if(friendlyTeam == matchup.first) matchup.second else matchup.first

        var friendlyHeads = Component.empty()
        val friendlyPlayers = friendlyTeam
            .getAllPlayers()
            .sortedByDescending { crumble.alivePlayers[it.team]?.contains(it) == true }
            .take(4)
        var heads = 0
        var skulls = 0
        friendlyPlayers.forEach {
            if(crumble.alivePlayers[it.team]?.contains(it) != true ) {
                friendlyHeads = friendlyHeads.append(Format.mm(" <glyph:icon/skull>"))
                skulls++
                return@forEach
            }

            friendlyHeads = friendlyHeads.append(Format.mm(" <head:${it.uuid}>"))
            heads++
        }

        if(friendlyPlayers.size < 4) {
            repeat(4 - friendlyPlayers.size) {
                friendlyHeads = friendlyHeads.append(Format.mm(" <dark_gray><head:606e2ff0-ed77-4842-9d6c-e1d3321c7838></dark_gray>"))
                heads++
            }
        }

        component = component.append(Format.mm(
            "<team:${friendlyTeam.name}:icon> <font:minecraft:default><heads></font>",
            Placeholder.component("heads", friendlyHeads)
        ))

        component = component.append(Component.text(" ".repeat(5)))
        val time = formatToMSS(crumble.countdownTime)
        component = component.append(Component.text(time))
        component = component.append(Component.text(" ".repeat(5)))

        var enemyHeads = Component.empty()
        val enemyPlayers = enemyTeam
            .getAllPlayers()
            .sortedByDescending { crumble.alivePlayers[it.team]?.contains(it) == true }
            .take(4)

        enemyPlayers.forEach {
            if(crumble.alivePlayers[it.team]?.contains(it) != true ) {
                enemyHeads = enemyHeads.append(Format.mm(" <glyph:icon/skull>"))
                skulls++
                return@forEach
            }

            enemyHeads = enemyHeads.append(Format.mm(" <head:${it.uuid}>"))
            heads++
        }

        if(enemyPlayers.size < 4) {
            repeat(4 - enemyPlayers.size) {
                enemyHeads = enemyHeads.append(Format.mm(" <dark_gray><head:606e2ff0-ed77-4842-9d6c-e1d3321c7838></dark_gray>"))
                heads++
            }
        }

        component = component.append(Format.mm(
            "<heads> <team:${enemyTeam.name}:icon>",
            Placeholder.component("heads", enemyHeads)
        ))

        val bgComponent = Component.empty()
            // for consistency since skulls are 1 pixel longer than the normal player heads
            .append(UserInterfaceUtility.positiveSpace(skulls/2))
            .append(UserInterfaceUtility.negativeSpace(2))
            .append(Font.getGlyph("hud/crumble_matchup_bossbar").shadowColor(ShadowColor.shadowColor(0)))
            .append(UserInterfaceUtility.negativeSpace(
                (
                    // Team icons
                    14.0
                    // Heads
                    + heads * headLength
                    // Spacing from heads
                    + heads * 4
                    // Skulls
                    + skulls * skullLength
                    // Spacing from skulls
                    + skulls * 4
                    // Spacing between the heads and the countdown
                    + 40
                    // The countdown
                    + UserInterfaceUtility.getPixelWidth(time)
                    // 1px spacing between characters
                    + (heads + skulls + 8 + time.length)
                ).roundToInt()
            ))
            .append(component)

        return bgComponent
    }
}