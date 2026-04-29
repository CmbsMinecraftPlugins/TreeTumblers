package xyz.devcmb.tumblers.ui.scoreboard.games



import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.games.breach.BreachController
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.tumblingPlayer

class BreachScoreboard(
    val gameController: GameController,
    val player: Player,
    override val id: String = "breachScoreboard"
) : HandledScoreboard {
    override fun getObjectives(scoreboard: Scoreboard): Set<Objective> {
        val activeGame = gameController.activeGame
        if(activeGame !is BreachController) return emptySet()

        val objective = scoreboard.registerNewObjective(
            "breachScoreboard",
            Criteria.create("dummy"),
            Format.mm("<yellow><b>Breach</b></yellow>")
        )

        objective.displaySlot = DisplaySlot.SIDEBAR

        val team = player.tumblingPlayer.team
        val playing = team == activeGame.playingTeams.first || team == activeGame.playingTeams.second

        val ingameSection = if (playing) arrayListOf(
            Component.empty(),
            Format.mm("<green>\uD83D\uDDE1 Kills: </green><white>${activeGame.kills.getOrDefault(player, 0)}</white>"),
            Format.mm("<red>\uD83D\uDC80 Deaths: </red><white>${activeGame.deaths.getOrDefault(player, 0)}</white>")
        ) else arrayListOf()

        MiscUtils.addScoreboardObjectiveLines(objective, arrayListOf(
            Component.empty(),
            Format.mm("<aqua>Round </aqua><white>${activeGame.currentRound}</white>"),
            Component.empty(),
            activeGame.playingTeams.first.formattedName.append(Format.mm(" <dark_gray>-</dark_gray> <gray>${activeGame.team1score}/${BreachController.bestOf}")),
            activeGame.playingTeams.second.formattedName.append(Format.mm(" <dark_gray>-</dark_gray> <gray>${activeGame.team2score}/${BreachController.bestOf}")),
            *ingameSection.toTypedArray(),
            Component.empty()
        ))

        return setOf(objective)
    }
}