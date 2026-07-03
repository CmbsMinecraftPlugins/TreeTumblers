package xyz.devcmb.tumblers.ui.scoreboard.games

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.deathrun.DeathrunController
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.getOrdinalSuffix
import xyz.devcmb.tumblers.util.tumblingPlayer

class DeathrunScoreboard(
    val player: Player,
) : HandledScoreboard.SidebarScoreboard() {
    override val displayName: String = "<yellow>Deathrun</yellow> <dark_gray>|</dark_gray> <gray>Game <game>/<total></gray>"
    override val id: String = "deathrunScoreboard"

    override fun getLines(): ArrayList<Component> {
        val activeGame = GameController.activeGame
        if(activeGame !is DeathrunController) return arrayListOf()

        val rounds = arrayListOf<Component>()
        repeat((activeGame.rounds + 3) / 4) { set ->
            var component = Component.empty()

            repeat(minOf(4, activeGame.rounds - (set * 4))) { setRound ->
                val roundIndex = (set * 4) + setRound
                val result = activeGame.placements[roundIndex][player.tumblingPlayer]

                component = component.append(when (result) {
                    null -> {
                        if(roundIndex < activeGame.roundIndex) Format.mm("${if(setRound != 0) " " else ""}<gray>[<yellow>DNF</yellow>]</gray>")
                        else Format.mm("${if (setRound != 0) " " else ""}<gray>[ ]</gray>")
                    }
                    -1 -> Format.mm("${if(setRound != 0) " " else ""}<gray>[<yellow>DNF</yellow>]</gray>")
                    -2 -> Format.mm("${if(setRound != 0) " " else ""}<gray>[<red>\uD83D\uDDE1</red>]</gray>")
                    else -> Format.mm("${if (setRound != 0) " " else ""}<gray>[<green>$result${getOrdinalSuffix(result)}</green>]</gray>")
                })
            }

            rounds.add(component)
        }

        val leaderboard: ArrayList<Component> = UserInterfaceUtility.getTeamScoresComponent(player, activeGame)
        return arrayListOf(
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
        )
    }
}