package xyz.devcmb.tumblers.engine.base

import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.subtitleCountdown
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
        spawn(SpawnCycle.PRE_ROUND)
        preRound = true
        playerCheck()
        // TODO: Replace with the `timer` syntax on the other branch
        asyncCountdown(10) {}

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
            asyncCountdown(roundLength) { isEarly ->
                if(!isEarly) {
                    onRoundTimeout()
                    roundActive = false
                }
            }
            startRound()

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
}