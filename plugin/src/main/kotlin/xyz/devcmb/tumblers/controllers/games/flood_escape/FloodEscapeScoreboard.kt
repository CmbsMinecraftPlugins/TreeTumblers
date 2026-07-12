package xyz.devcmb.tumblers.controllers.games.flood_escape

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.getOrdinalSuffix
import xyz.devcmb.tumblers.util.tumblingPlayer
import kotlin.math.roundToInt

class FloodEscapeScoreboard(
    val player: Player,
    gameData: GameData,
) : HandledScoreboard.GameScoreboard(gameData, NamedTextColor.BLUE) {
    val placementTemplate: String = "<placement>. <player> <gray>-</gray> <white><distance></white>"

    override fun getLines(): ArrayList<Component> {
        val activeGame = GameController.activeGame as? FloodEscapeController ?: return arrayListOf()

        var roundsComponent = Component.empty()
        repeat(activeGame.rounds) {
            val placement = activeGame.playerPlacements[it][player.tumblingPlayer]
            roundsComponent = roundsComponent.append(
                Format.mm(
                    "${if(it != 0) " " else ""}<gray>[${if(placement != null) "<green>$placement${
                        getOrdinalSuffix(
                            placement
                        )
                    }</green>" else " "}]</gray>"
                )
            )
        }

        val leaderboard: ArrayList<Component> = ArrayList()
        val distances = activeGame.playerDistances.toList()
            .sortedWith(
                compareByDescending<Pair<TumblingPlayer, Double>> { it.second }
                    .thenBy { it.first.team.priority }
            )
        val top = distances.take(3)

        if(!top.isEmpty()) {
            if(!top.any { it.first == player.tumblingPlayer } && player.tumblingPlayer.team.playingTeam) {
                val topPlayer = top.first()
                leaderboard.add(
                    Format.mm(
                    placementTemplate,
                    Placeholder.unparsed("placement", "1"),
                    Placeholder.component("player",
                        if(topPlayer.first !in activeGame.alivePlayers) topPlayer.first.eliminatedName else topPlayer.first.formattedName
                    ),
                    Placeholder.unparsed("distance", topPlayer.second.roundToInt().toString())
                ))

                leaderboard.add(Component.empty())

                var playerDistanceIndex = distances.indexOfFirst { it.first == player.tumblingPlayer }
                if(playerDistanceIndex == -1) playerDistanceIndex = distances.size + 1

                val closestDistances = arrayListOf(
                    distances.getOrNull(playerDistanceIndex - 1),
                    distances[playerDistanceIndex],
                    distances.getOrNull(playerDistanceIndex + 1)
                )

                val nextPlacement = distances.getOrNull(playerDistanceIndex - 2)
                if(closestDistances.filterNotNull().size != 3 && nextPlacement != null) {
                    closestDistances.addFirst(nextPlacement)
                }

                closestDistances.filterNotNull().forEach {
                    val (plr) = it
                    leaderboard.add(
                        Format.mm(
                        placementTemplate,
                        Placeholder.component("player",
                            if(plr !in activeGame.alivePlayers) plr.eliminatedName else plr.formattedName
                        ),
                        Placeholder.unparsed("placement", (distances.indexOfFirst { d -> d.first == plr } + 1).toString()),
                        Placeholder.unparsed("distance", it.second.roundToInt().toString())
                    ))
                }
            } else {
                val top4 = distances.take(4)
                top4.forEach {
                    leaderboard.add(
                        Format.mm(
                        placementTemplate,
                        Placeholder.component("player",
                            if(it.first !in activeGame.alivePlayers) it.first.eliminatedName else it.first.formattedName
                        ),
                        Placeholder.unparsed("placement", (distances.indexOfFirst { d -> d.first == it.first } + 1).toString()),
                        Placeholder.unparsed("distance", it.second.roundToInt().toString())
                    ))
                }
            }
        }

        return arrayListOf(
            Component.empty(),
            UserInterfaceUtility.timer(activeGame),
            Format.mm(
                MiniMessagePlaceholders.Game.SCOREBOARD_CURRENT_ROUND,
                Placeholder.unparsed("current", activeGame.currentRound.toString()),
                Placeholder.unparsed("total", activeGame.rounds.toString())
            ),
            roundsComponent,
            Format.mm("<color:${MiniMessagePlaceholders.Event.EVENT_COLOR}><white>Water Speed:</white> ${(activeGame.waterSpeed / activeGame.startingSpeed * 100).roundToInt()}%</color>"),
            Component.empty(),
            *leaderboard.toTypedArray(),
            Component.empty(),
            Format.mm("<white>Obstacles Completed: <green>${
                activeGame.playerObstacles[player.tumblingPlayer]?.let { 
                    activeGame.obstacles[activeGame.roundIndex][it].index + 1 
                } ?: 0
            }</green></white>"),
            Component.empty(),
        )
    }
}