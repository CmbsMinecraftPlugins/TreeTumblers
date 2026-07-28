package xyz.devcmb.tumblers.ui.bossbar.games.breach

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.breach.BreachController
import xyz.devcmb.tumblers.ui.bossbar.HandledBossbar
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Font
import xyz.devcmb.tumblers.util.tumblingPlayer

class ScoreBossbar(val player: Player) : HandledBossbar {
    override val id: String = "breachScoreBossbar"
    override val padding: Int = 0

    override fun getComponent(): Component {
        val activeGame = GameController.activeGame
        if(activeGame == null || activeGame !is BreachController) return DebugUtil.DebugLogLevel.ERROR.icon()

        val team1 =
            if(player.tumblingPlayer.team == activeGame.playingTeams.first) activeGame.playingTeams.first
            else activeGame.playingTeams.second
        val team2 =
            if(player.tumblingPlayer.team == activeGame.playingTeams.first) activeGame.playingTeams.second
            else activeGame.playingTeams.first

        val team1Score =
            if(player.tumblingPlayer.team == activeGame.playingTeams.first) activeGame.team1score
            else activeGame.team2score
        val team2Score =
            if(player.tumblingPlayer.team == activeGame.playingTeams.second) activeGame.team1score
            else activeGame.team2score

        var component = team1.formattedIcon
        component = component.append(Component.text(" "))

        repeat(team1Score) {
            component = component.append(Font.getGlyph("hud/breach/star", false).color(team1.color))
        }

        repeat(BreachController.bestOf - team1Score) {
            component = component.append(Font.getGlyph("hud/breach/star", false).color(NamedTextColor.DARK_GRAY))
        }

        component = component.append(Font.getGlyph("hud/breach/nether_star"))

        repeat(BreachController.bestOf - team2Score) {
            component = component.append(Font.getGlyph("hud/breach/star", false).color(NamedTextColor.DARK_GRAY))
        }

        repeat(team2Score) {
            component = component.append(Font.getGlyph("hud/breach/star", false).color(team2.color))
        }

        component = component.append(Component.text(" "))
        component = component.append(team2.formattedIcon)

        return component
    }
}