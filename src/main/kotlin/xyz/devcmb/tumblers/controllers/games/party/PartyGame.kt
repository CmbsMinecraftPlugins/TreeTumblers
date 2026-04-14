package xyz.devcmb.tumblers.controllers.games.party

import io.papermc.paper.util.Tick
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.Kit
import xyz.devcmb.tumblers.util.hideToAll
import xyz.devcmb.tumblers.util.tumblingPlayer

abstract class PartyGame(
    val partyController: PartyController?,
    val matchup: PartyController.PartyMatchup
) : Listener {
    abstract val id: String
    abstract val kit: Kit.KitDefinition

    abstract val team: Boolean
    abstract val individual: Boolean

    abstract fun postSpawn()
    abstract suspend fun start()
    abstract fun cleanup()

    val alivePlayers: MutableSet<Player> = mutableSetOf(*matchup.players.toTypedArray())

    val victoryTitle = Title.title(
        Format.mm("<green><b>Game won!</b></green>"),
        Format.mm("Well played!"),
        Title.Times.times(Tick.of(0), Tick.of(30), Tick.of(5))
    )

    val defeatTitle = Title.title(
        Format.mm("<red><b>Game lost!</b></red>"),
        Format.mm("Better luck next time!"),
        Title.Times.times(Tick.of(0), Tick.of(30), Tick.of(5))
    )

    val drawTitle = Title.title(
        Format.mm("<yellow><b>Game draw!</b></yellow>"),
        Component.empty(),
        Title.Times.times(Tick.of(0), Tick.of(30), Tick.of(5))
    )

    private fun win(side: MatchupSide) {
        when(matchup) {
            is PartyController.PartyMatchup.IndividualMatchup -> {
                val winner = if(side == MatchupSide.FIRST) matchup.player1 else matchup.player2
                winner!!.showTitle(victoryTitle)
                partyController!!.grantScore(winner, PartyController.PartyScoreSource.INDIVIDUAL_GAME_WIN)
            }
            is PartyController.PartyMatchup.TeamMatchup -> {
                val winner = if(side == MatchupSide.FIRST) matchup.team1 else matchup.team2
                winner.audience.showTitle(victoryTitle)
                partyController!!.grantTeamScore(winner, PartyController.PartyScoreSource.TEAM_GAME_WIN)
            }
        }
    }

    private fun lose(side: MatchupSide) {
        when(matchup) {
            is PartyController.PartyMatchup.IndividualMatchup -> {
                val winner = if(side == MatchupSide.FIRST) matchup.player1 else matchup.player2
                winner!!.showTitle(defeatTitle)
                partyController!!.grantScore(winner, PartyController.PartyScoreSource.INDIVIDUAL_GAME_LOSE)
            }
            is PartyController.PartyMatchup.TeamMatchup -> {
                val winner = if(side == MatchupSide.FIRST) matchup.team1 else matchup.team2
                winner.audience.showTitle(defeatTitle)
                partyController!!.grantTeamScore(winner, PartyController.PartyScoreSource.TEAM_GAME_LOSE)
            }
        }
    }

    private fun draw() {
        when(matchup) {
            is PartyController.PartyMatchup.IndividualMatchup -> {
                matchup.player1!!.showTitle(drawTitle)
                matchup.player2?.showTitle(drawTitle)

                partyController!!.grantScore(matchup.player1, PartyController.PartyScoreSource.INDIVIDUAL_GAME_DRAW)
                matchup.player2?.let { partyController.grantScore(it, PartyController.PartyScoreSource.INDIVIDUAL_GAME_DRAW) }
            }
            is PartyController.PartyMatchup.TeamMatchup -> {
                Audience.audience(matchup.team1.audience, matchup.team2.audience).showTitle(drawTitle)

                partyController!!.grantTeamScore(matchup.team1, PartyController.PartyScoreSource.TEAM_GAME_DRAW)
                partyController.grantTeamScore(matchup.team2, PartyController.PartyScoreSource.TEAM_GAME_DRAW)
            }
        }
    }

    @EventHandler
    fun playerKillEvent(event: PlayerDeathEvent) {
        val player = event.player

        player.hideToAll()
        player.heal(20.0)
        player.inventory.clear()
        player.allowFlight = true
        player.isFlying = true
        event.isCancelled = true

        alivePlayers.remove(player)
        if(!alivePlayers.any { it.tumblingPlayer.team == player.tumblingPlayer.team }) {
            when(matchup) {
                is PartyController.PartyMatchup.IndividualMatchup -> {
                    val winningSide = if(matchup.player1 == player) MatchupSide.SECOND else MatchupSide.FIRST
                    val otherSide = if(winningSide == MatchupSide.FIRST) MatchupSide.SECOND else MatchupSide.FIRST

                    if(
                        (winningSide == MatchupSide.FIRST && matchup.player1 !in alivePlayers)
                        || (winningSide == MatchupSide.SECOND && (matchup.player2 == null || matchup.player2 !in alivePlayers))
                    ) {
                        draw()
                    } else {
                        win(winningSide)
                        lose(otherSide)
                    }
                }
                is PartyController.PartyMatchup.TeamMatchup -> {
                    val winningSide = if(player in matchup.team1.getOnlinePlayers()) MatchupSide.SECOND else MatchupSide.FIRST
                    val otherSide = if(winningSide == MatchupSide.FIRST) MatchupSide.SECOND else MatchupSide.FIRST

                    if(
                        (winningSide == MatchupSide.FIRST && !matchup.team1.getOnlinePlayers().any { it in alivePlayers })
                        || (winningSide == MatchupSide.SECOND && !matchup.team2.getOnlinePlayers().any { it in alivePlayers })
                    ) {
                        draw()
                    } else {
                        win(winningSide)
                        lose(otherSide)
                    }
                }
            }
            partyController!!.endGame(this)
        }
    }

    enum class MatchupSide {
        FIRST,
        SECOND
    }
}