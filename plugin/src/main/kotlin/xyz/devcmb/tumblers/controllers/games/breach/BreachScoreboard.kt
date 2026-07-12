package xyz.devcmb.tumblers.controllers.games.breach

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.base.AbstractGame
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class BreachScoreboard(
    val player: Player,
    gameData: GameData,
) : HandledScoreboard.GameScoreboard(gameData, Team.ORANGE.color) {
    override fun getLines(): ArrayList<Component> {
        val activeGame = GameController.activeGame
        if(activeGame !is BreachController) return arrayListOf()

        val team = player.tumblingPlayer.team
        val playing = team == activeGame.playingTeams.first || team == activeGame.playingTeams.second

        val ingameSection = if (playing) arrayListOf(
            Component.empty(),
            Format.mm(" <white>Kills: <green>${activeGame.kills.getOrDefault(player.tumblingPlayer, 0)}</green></white>"),
            Format.mm(" <white>Deaths: <red>${activeGame.deaths.getOrDefault(player.tumblingPlayer, 0)}</red></white>")
        ) else arrayListOf()

        return arrayListOf(
            Component.empty(),
            UserInterfaceUtility.timer(activeGame),
            Format.mm("<color:${MiniMessagePlaceholders.Event.EVENT_COLOR}><white>Round</white> ${activeGame.currentRound}</color>"),
            Component.empty(),
            activeGame.playingTeams.first.formattedName.append(Format.mm(" <dark_gray>-</dark_gray> <gray>${activeGame.team1score}/${BreachController.bestOf}")),
            activeGame.playingTeams.second.formattedName.append(Format.mm(" <dark_gray>-</dark_gray> <gray>${activeGame.team2score}/${BreachController.bestOf}")),
            *ingameSection.toTypedArray(),
            Component.empty()
        )
    }
}