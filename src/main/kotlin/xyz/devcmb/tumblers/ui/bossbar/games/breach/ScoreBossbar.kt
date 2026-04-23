package xyz.devcmb.tumblers.ui.bossbar.games.breach

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.games.breach.BreachController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.bossbar.HandledBossbar
import xyz.devcmb.tumblers.util.DebugUtil

class ScoreBossbar(
    val gameController: GameController,
    override val id: String = "breachScoreBossbar",
    override val padding: Int = 0
) : HandledBossbar {
    override fun getComponent(): Component {
        val activeGame = gameController.activeGame
        if(activeGame == null || activeGame !is BreachController) return Component.text(DebugUtil.DebugLogLevel.ERROR.icon).font(UserInterfaceUtility.WARNINGS)

        var component = Component.text(activeGame.playingTeams.first.icon).font(UserInterfaceUtility.ICONS)
        component = component.append(Component.text(" "))

        repeat(activeGame.team1score) {
            component = component.append(Component.text("\uEF00").font(BreachController.font).color(activeGame.playingTeams.first.color))
        }

        repeat(BreachController.bestOf - activeGame.team1score) {
            component = component.append(Component.text("\uEF00").font(BreachController.font).color(NamedTextColor.DARK_GRAY))
        }

        component = component.append(Component.text("\uEF01").font(BreachController.font))

        repeat(BreachController.bestOf - activeGame.team2score) {
            component = component.append(Component.text("\uEF00").font(BreachController.font).color(NamedTextColor.DARK_GRAY))
        }

        repeat(activeGame.team2score) {
            component = component.append(Component.text("\uEF00").font(BreachController.font).color(activeGame.playingTeams.second.color))
        }

        component = component.append(Component.text(" "))
        component = component.append(Component.text(activeGame.playingTeams.second.icon).font(UserInterfaceUtility.ICONS))

        return component
    }
}