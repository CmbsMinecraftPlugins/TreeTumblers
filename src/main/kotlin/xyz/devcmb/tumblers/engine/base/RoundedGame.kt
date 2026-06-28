package xyz.devcmb.tumblers.engine.base

import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.Timer
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.getOrdinalSuffix
import xyz.devcmb.tumblers.util.subtitleCountdown
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.tumblingPlayer
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

abstract class RoundedGame(
    override val data: GameData,
    val rounds: Int,
    val roundLength: Duration
) : AbstractGame(data) {
    constructor(data: GameData, rounds: Int, roundLength: Int)
        : this(data, rounds, roundLength.toDuration(DurationUnit.SECONDS))

    var currentRound = 0
        private set
    val roundIndex: Int
        get() { return currentRound - 1 }
    var preRound: Boolean = false
        private set
    var roundActive: Boolean = false
        private set

    abstract suspend fun startRound()

    /**
     * Gets the subtitle for the round announcement title
     *
     * @param player The player the title is being rendered for
     * @return The component to display
     */
    open fun getRoundAnnouncementSubtitle(player: Player): Component {
        return Component.empty()
    }

    /** Displays a round start message */
    open suspend fun preRound() {
        suspendSync {
            participatingSpectators.toList().forEach(this::unSpectate)
        }

        spawn(SpawnCycle.PRE_ROUND)
        preRound = true
        timer(Timer(10) {
            id = "${data.id}_round_start_timer"
            title = "${if(currentRound == 1) "Game" else "Round"} Start"
        })
        playerCheck()

        delay(2000)

        val title = Format.mm("<yellow><b>Round $currentRound</b></yellow>")
        gamePlayers.mapNotNull { it.bukkitPlayer }.forEach {
            it.showTitle(Title.title(
                title,
                getRoundAnnouncementSubtitle(it),
                Title.Times.times(Tick.of(3), Tick.of(999), Tick.of(0))
            ))
        }

        delay(3000)

        subtitleCountdown(Audience.audience(gamePlayers.mapNotNull { it.bukkitPlayer }), title, 5)
        preRound = false
    }

    open suspend fun postRound() {
        delay(1000)

        gamePlayers.mapNotNull { it.bukkitPlayer }.forEach {
            it.showTitle(Title.title(
                Format.mm("<red><b>Round Over!</b></red>"),
                Component.empty(),
                Title.Times.times(Tick.of(0), Tick.of(50), Tick.of(0))
            ))
        }

        delay(3000)
    }

    /**
     * Determines what to do when the timer runs out
     */
    open fun onRoundTimeout() {
    }

    override suspend fun gameOn() {
        repeat(rounds) {
            TreeTumblers.pluginScope.ensureActive()

            currentRound++
            preRound()
            roundActive = true
            timer(Timer(roundLength) {
                id = "${data.id}_round_on_timer"
                title = "${if(currentRound == rounds) "Game" else "Round"} Over"
                onComplete { isEarly ->
                    if(!isEarly) {
                        onRoundTimeout()
                        roundActive = false
                    }
                }
            })
            startRound()

            delay(500)
            while(roundActive) {
                delay(500)
            }

            postRound()
        }
    }

    suspend fun endRound() {
        cancelCountdown()
        roundActive = false
    }

    override suspend fun postGame() {
        val placements = getTeamPlacements()
        gameParticipants.mapNotNull { it.bukkitPlayer }.forEach { plr ->
            val teamPlacement = placements.find { it.first == plr.tumblingPlayer.team }!!.second

            val color = when(teamPlacement) {
                1 -> NamedTextColor.GOLD
                2 -> TextColor.fromHexString("#E0E0E0")
                3 -> TextColor.fromHexString("#CE8946")
                else -> NamedTextColor.AQUA
            }

            plr.showTitle(Title.title(
                Component.text("Game Over!", NamedTextColor.RED).decorate(TextDecoration.BOLD),
                Format.mm("<white>Team <color:${color!!.asHexString()}>$teamPlacement${getOrdinalSuffix(teamPlacement)}</color> place!"),
                Title.Times.times(Tick.of(3), Tick.of(90), Tick.of(3))
            ))
            plr.sendMessage(gameMessage(Component.text("Game Over!")))
        }

        gamePlayers.filter { !it.team.playingTeam }.mapNotNull { it.bukkitPlayer }.forEach { plr ->
            plr.showTitle(Title.title(
                Component.text("Game Over!", NamedTextColor.RED).decorate(TextDecoration.BOLD),
                Component.empty(),
                Title.Times.times(Tick.of(3), Tick.of(90), Tick.of(3))
            ))
            plr.sendMessage(gameMessage(Component.text("Game Over!")))
        }

        delay(5000)
        announceTeamScores()
        announceIndivScores()
        announceOverallTeamScores()
    }
}