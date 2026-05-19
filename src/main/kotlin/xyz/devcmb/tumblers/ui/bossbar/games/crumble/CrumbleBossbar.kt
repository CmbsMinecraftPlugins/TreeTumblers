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
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.formatToMSS
import xyz.devcmb.tumblers.util.tumblingPlayer

class CrumbleBossbar(
    val player: Player,
    val gameController: GameController,
    override val id: String = "crumbleBossbar",
    override val padding: Int = 0
) : HandledBossbar {
    override fun getComponent(): Component {
        val crumble = gameController.activeGame
            as? CrumbleController
            ?: return Component.text(DebugUtil.DebugLogLevel.ERROR.icon).font(UserInterfaceUtility.WARNINGS)

        var component = Component.empty()
        val matchup = crumble.getCurrentMatchup(player) ?: return Component.empty()

        val friendlyTeam = if(matchup.first == player.tumblingPlayer.team) matchup.first else matchup.second
        val enemyTeam = if(friendlyTeam == matchup.first) matchup.second else matchup.first

        var friendlyHeads = Component.empty()
        val friendlyPlayers = friendlyTeam
            .getAllPlayers()
            .sortedByDescending { crumble.alivePlayers[it.team]?.contains(it) == true }
            .take(4)
        friendlyPlayers.forEach {
            if(crumble.alivePlayers[it.team]?.contains(it) != true ) {
                friendlyHeads = friendlyHeads.append(Format.mm(" 💀"))
                return@forEach
            }

            friendlyHeads = friendlyHeads.append(Format.mm(" <head:${it.uuid}>"))
        }

        if(friendlyPlayers.size < 4) {
            repeat(4 - friendlyPlayers.size) {
                friendlyHeads = friendlyHeads.append(Format.mm(" <dark_gray><head:606e2ff0-ed77-4842-9d6c-e1d3321c7838></dark_gray>"))
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
                enemyHeads = enemyHeads.append(Format.mm(" 💀"))
                return@forEach
            }

            enemyHeads = enemyHeads.append(Format.mm(" <head:${it.uuid}>"))
        }

        if(enemyPlayers.size < 4) {
            repeat(4 - enemyPlayers.size) {
                enemyHeads = enemyHeads.append(Format.mm(" <dark_gray><head:606e2ff0-ed77-4842-9d6c-e1d3321c7838></dark_gray>"))
            }
        }

        component = component.append(Format.mm(
            "<heads> <team:${enemyTeam.name}:icon>",
            Placeholder.component("heads", enemyHeads)
        ))

        val bgComponent = Component.empty()
            .append(UserInterfaceUtility.negativeSpace(2))
            .append(Component.text("\uEF03").font(CrumbleController.font).shadowColor(ShadowColor.shadowColor(0)))
            .append(UserInterfaceUtility.negativeSpace(191))
            .append(component)

        return bgComponent
    }
}