package xyz.devcmb.tumblers.ui.scoreboard.games

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.brawl.BrawlController
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.getOrdinalSuffix
import xyz.devcmb.tumblers.util.tumblingPlayer

class BrawlScoreboard(
    val player: Player,
) : HandledScoreboard.SidebarScoreboard() {
    override val id: String = "brawlScoreboard"
    override val displayName: String = "<green>Brawl</green> <dark_gray>|</dark_gray> <gray>Game <game>/<total></gray>"

    override fun getLines(): ArrayList<Component> {
        val activeGame = GameController.activeGame as? BrawlController ?: return arrayListOf()

        var roundsComponent = Component.empty()
        repeat(activeGame.rounds) {
            val placement = activeGame.roundPlacements[it][player.tumblingPlayer]
            roundsComponent = roundsComponent.append(if(placement == -1) Format.mm(
                "${if(it != 0) " " else ""}<gray>[<white><glyph:icon/trophy></white>]</gray>"
            ) else Format.mm(
                "${if(it != 0) " " else ""}<gray>[${if(placement != null) "<green>$placement${getOrdinalSuffix(placement)}</green>" else " "}]</gray>"
            ))
        }

        return arrayListOf(
            Component.empty(),
            Format.mm(
                "<color:${MiniMessagePlaceholders.Event.EVENT_COLOR}><white>${activeGame.currentTimer?.title ?: "Timer"}:</white> <timer></color>",
                Placeholder.component("timer", activeGame.currentTimer?.format() ?: Component.text("0:00"))
            ),
            Format.mm(
                MiniMessagePlaceholders.Game.SCOREBOARD_CURRENT_ROUND,
                Placeholder.unparsed("current", activeGame.currentRound.toString()),
                Placeholder.unparsed("total", activeGame.rounds.toString())
            ),
            roundsComponent,
            Component.empty(),
            *UserInterfaceUtility.getTeamScoresComponent(player, activeGame).toTypedArray(),
            Component.empty(),
            UserInterfaceUtility.getIndividualScoreComponent(player, activeGame),
            Component.empty()
        )
    }
}