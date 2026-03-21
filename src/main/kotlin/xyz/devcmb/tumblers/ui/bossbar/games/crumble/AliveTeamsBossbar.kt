package xyz.devcmb.tumblers.ui.bossbar.games.crumble

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.bossbar.HandledBossbar
import kotlin.math.roundToInt

class AliveTeamsBossbar(
    val gameController: GameController,
    override val id: String = "crumbleAliveTeamsBossbar",
    override val padding: Int = 0
) : HandledBossbar {
    override fun getComponent(): Component {
        val activeGame = gameController.activeGame
        if(activeGame == null || activeGame !is CrumbleController) return Component.text("\uEF00").font(UserInterfaceUtility.ICONS)

        val playingTeams = Team.entries.filter { it.playingTeam }

        val bgSize = 100.0
        val badgeSize = 9.0
        val textLength = (playingTeams.size * badgeSize) + playingTeams.size
        val bgOffset = (textLength+((bgSize - textLength)/2)).roundToInt()
        val fullOffset = ((bgSize - textLength) / 2).roundToInt()

        var teamComponent = Component.empty()
            .append(UserInterfaceUtility.negativeSpace(fullOffset))
            .append(Component.text("\uEF01").font(CrumbleController.font))
            .append(UserInterfaceUtility.negativeSpace(bgOffset))
        playingTeams.forEach { team ->
            teamComponent = teamComponent.append(Component.text(
                if(activeGame.alivePlayers[team]!!.isEmpty()) "\uEF00"
                else team.icon
            ).font(UserInterfaceUtility.ICONS))
        }
        teamComponent = teamComponent.shadowColor(ShadowColor.shadowColor(0))

        return teamComponent
    }
}