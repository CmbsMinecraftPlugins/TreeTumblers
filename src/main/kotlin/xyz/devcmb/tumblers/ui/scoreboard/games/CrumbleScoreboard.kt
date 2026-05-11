package xyz.devcmb.tumblers.ui.scoreboard.games

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class CrumbleScoreboard(
    val gameController: GameController,
    val player: Player,
    override val displayName: Component = Format.mm(
        MiniMessagePlaceholders.Game.SCOREBOARD_TITLE,
        Placeholder.unparsed("name", "Crumble")
    ),
    override val id: String = "crumbleScoreboard"
) : HandledScoreboard.SidebarScoreboard() {
    override fun getLines(): ArrayList<Component> {
        val activeGame = gameController.activeGame
        if(activeGame !is CrumbleController) return arrayListOf()

        var rounds = Format.mm("<aqua>Rounds: </aqua>")
        repeat(activeGame.rounds) {
            val result = activeGame.matchResults[it][player.tumblingPlayer.team]
            val append = when(result) {
                CrumbleController.RoundResult.WIN -> "<white>[<green>W</green>]</white>"
                CrumbleController.RoundResult.DRAW -> "<white>[<yellow>D</yellow>]</white>"
                CrumbleController.RoundResult.LOSS -> "<white>[<red>L</red>]</white>"
                null -> "<white>[ ]</white>"
            }
            rounds = rounds.append(Format.mm("${if(it != 0) " " else ""}$append"))
        }

        val leaderboard: ArrayList<Component> = UserInterfaceUtility.getTeamScoresComponent(player, activeGame)
        return arrayListOf(
            Component.empty(),
            Format.mm(
                MiniMessagePlaceholders.Game.SCOREBOARD_CURRENT_ROUND,
                Placeholder.unparsed("current", activeGame.currentRound.toString()),
                Placeholder.unparsed("total", activeGame.rounds.toString())
            ),
            rounds,
            Component.empty(),
            *leaderboard.toTypedArray(),
            Component.empty(),
            UserInterfaceUtility.getIndividualScoreComponent(player, activeGame),
            Component.empty()
        )
    }
}