package xyz.devcmb.tumblers.controllers.games.tower_ascent

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class TowerAscentScoreboard(
    val player: Player,
    gameData: GameData
) : HandledScoreboard.GameScoreboard(gameData, NamedTextColor.LIGHT_PURPLE) {
    override fun getLines(): ArrayList<Component> {
        val game = GameController.activeGame as? TowerAscentController ?: return arrayListOf()

        val room = game.teamRooms[player.tumblingPlayer.team]
        val lines = arrayListOf(
            Component.empty(),
            UserInterfaceUtility.timer(game),
        )
        room?.let {
            lines.add(Format.mm("<white>Room <color:${MiniMessagePlaceholders.Event.EVENT_COLOR}>${it + 1}/${game.generator.roomCount}</color></white>"))
        }
        lines.add(Component.empty())

        val leaderboard = arrayListOf<Component>()
        val placements = calculatePlacements()
        val playerTeamPlacement = placements.indexOf(player.tumblingPlayer.team) + 1
        if(playerTeamPlacement > 3) {
            leaderboard.add(getRoomsLeaderboardComponent(placements.first(), 1))
            leaderboard.add(Component.empty())
        }

        placements.forEachIndexed { placementIndex, team ->
            val placement = placementIndex + 1
            val bound =
                if(playerTeamPlacement == 0 || playerTeamPlacement <= 3) (1..4)
                else if(playerTeamPlacement == Team.playingTeams.size) (playerTeamPlacement - 2)..(playerTeamPlacement)
                else ((playerTeamPlacement - 1) .. (playerTeamPlacement + 1))

            if(placement !in bound) return@forEachIndexed

            leaderboard.add(getRoomsLeaderboardComponent(team, placement))
        }
        lines.addAll(leaderboard)
        lines.add(Component.empty())

        return lines
    }

    private fun getRoomsLeaderboardComponent(team: Team, placement: Int): Component {
        val game = GameController.activeGame as? TowerAscentController ?: return Component.empty()
        return Format.mm(
            "<white>$placement.</white> <team> <dark_gray>-</dark_gray> <aqua>${game.teamCompletedRooms[team] ?: 0}</aqua><white><glyph:icon/flag_blue></white>",
            Placeholder.component("team", team.formattedName)
        )
    }

    // ai code
    private fun calculatePlacements(): List<Team> {
        val game = GameController.activeGame as? TowerAscentController ?: return arrayListOf()
        return game.teamCompletedRooms.keys.sortedWith(
            compareByDescending<Team> { game.teamCompletedRooms[it] ?: 0 }
                .thenComparator { a, b ->
                    val aPlacements = game.teamRoomPlacements[a] ?: emptyList()
                    val bPlacements = game.teamRoomPlacements[b] ?: emptyList()

                    val maxRooms = minOf(aPlacements.size, bPlacements.size)

                    for (i in 0 until maxRooms) {
                        if (aPlacements[i] != bPlacements[i]) {
                            return@thenComparator aPlacements[i].compareTo(bPlacements[i])
                        }
                    }

                    0
                }
                .thenBy { it.priority }
        )
    }
}