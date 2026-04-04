package xyz.devcmb.tumblers.ui.scoreboard.games

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.games.deathrun.DeathrunController
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils

class DeathrunScoreboard(
    val gameController: GameController,
    val player: Player,
    override val id: String = "deathrunScoreboard"
) : HandledScoreboard {
    override fun getObjectives(scoreboard: Scoreboard): Set<Objective> {
        val activeGame = gameController.activeGame
        if(activeGame !is DeathrunController) return emptySet()

        val objective = scoreboard.registerNewObjective(
            "deathrunGameScoreboard",
            Criteria.create("dummy"),
            Format.mm(
                MiniMessagePlaceholders.Game.SCOREBOARD_TITLE,
                Placeholder.unparsed("name", "Deathrun")
            )
        )
        objective.displaySlot = DisplaySlot.SIDEBAR

        val rounds = arrayListOf<Component>()
        repeat((activeGame.rounds + 3) / 4) { set ->
            var component = Component.empty()

            repeat(minOf(4, activeGame.rounds - (set * 4))) { setRound ->
                val roundIndex = (set * 4) + setRound
                val result = activeGame.placements[roundIndex][player]

                component = component.append(when (result) {
                    null -> Format.mm("${if(setRound != 0) " " else ""}<white>[ ]</white>")
                    -1 -> Format.mm("${if(setRound != 0) " " else ""}<white>[<yellow>DNF</yellow>]</white>")
                    -2 -> Format.mm("${if(setRound != 0) " " else ""}<white>[<red>\uD83D\uDDE1</red>]</white>")
                    else -> Format.mm("${if (setRound != 0) " " else ""}<white>[<green>$result${MiscUtils.getOrdinalSuffix(result)}</green>]</white>")
                })
            }

            rounds.add(component)
        }

        val leaderboard: ArrayList<Component> = UserInterfaceUtility.getTeamScoresComponent(player, activeGame)
        MiscUtils.addScoreboardObjectiveLines(objective, arrayListOf(
            Component.empty(),
            Format.mm(
                MiniMessagePlaceholders.Game.SCOREBOARD_CURRENT_ROUND,
                Placeholder.unparsed("current", activeGame.currentRound.toString()),
                Placeholder.unparsed("total", activeGame.rounds.toString())
            ),
            *rounds.toTypedArray(),
            Component.empty(),
            *leaderboard.toTypedArray(),
            Component.empty(),
            UserInterfaceUtility.getIndividualScoreComponent(player, activeGame),
            Component.empty()
        ))

        return setOf(objective)
    }
}