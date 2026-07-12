package xyz.devcmb.tumblers.ui.bossbar.games.breach

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.breach.BreachController
import xyz.devcmb.tumblers.ui.bossbar.HandledBossbar
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Font

class ScoreBossbar : HandledBossbar {
    override val id: String = "breachScoreBossbar"
    override val padding: Int = 0

    override fun getComponent(): Component {
        val activeGame = GameController.activeGame
        if(activeGame == null || activeGame !is BreachController) return DebugUtil.DebugLogLevel.ERROR.icon()

        var component = activeGame.playingTeams.first.formattedIcon
        component = component.append(Component.text(" "))

        repeat(activeGame.team1score) {
            component = component.append(Font.getGlyph("hud/breach/star", false).color(activeGame.playingTeams.first.color))
        }

        repeat(BreachController.bestOf - activeGame.team1score) {
            component = component.append(Font.getGlyph("hud/breach/star", false).color(NamedTextColor.DARK_GRAY))
        }

        component = component.append(Font.getGlyph("hud/breach/nether_star"))

        repeat(BreachController.bestOf - activeGame.team2score) {
            component = component.append(Font.getGlyph("hud/breach/star", false).color(NamedTextColor.DARK_GRAY))
        }

        repeat(activeGame.team2score) {
            component = component.append(Font.getGlyph("hud/breach_star", false).color(activeGame.playingTeams.second.color))
        }

        component = component.append(Component.text(" "))
        component = component.append(activeGame.playingTeams.second.formattedIcon)

        return component
    }
}