package xyz.devcmb.tumblers.controllers.games.deathrun

import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.enableBossBar
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.unpackCoordinates

@EventGame
class DeathrunController : GameBase(
    id = "deathrun",
    votable = true,
    maps = setOf(
        Map("forest")
    ),
    cutsceneSteps = arrayListOf(
        CutsceneStep(Component.empty()
            .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
            .append(Component.text("\uEA00").font(NamespacedKey("tumbling", "games/crumble")))
            .append(Component.text(" Deathrun"))
        ) {
            teleportConfig("cutscene.start")
            delay(5000)
        }
    ),
    flags = setOf(
        Flag.DISABLE_FALL_DAMAGE,
        Flag.DISABLE_PVP
    ),
    scores = hashMapOf(),
    icon = Component.empty(),
    scoreboard = "deathrunScoreboard"
) {
    val playingTeams = Team.entries.filter { it.playingTeam }
    val rounds = playingTeams.size
    var currentRound = 0
    val roundIndex
        get() = currentRound - 1

    val currentMap: LoadedMap
        get() {
            return loadedMaps[roundIndex]
        }

    val currentTeam: Team
        get() {
            return playingTeams[roundIndex]
        }
    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    override suspend fun gameLoad() {
        repeat(rounds) {
            loadMap(maps.random(), it + 1)
        }
    }

    override suspend fun gamePregame() {
    }

    /**
     * The abstract method for spawning players in
     *
     * There was going to be some kind of system to do this automatically, but doing it manually seems to be a more flexible option, at least for now.
     *
     * @param cycle The stage where the players are spawned
     */
    override suspend fun spawn(cycle: SpawnCycle) {
        if(cycle != SpawnCycle.PRE_ROUND) return

        val mainSpawn: List<Double> = currentMap.data.getList("spawns.main")?.map {
            if(it !is Double) throw GameControllerException("Spawn locations do not contain exclusively doubles")
            it
        } ?: throw GameControllerException("Main spawn set not found")

        val attackerSpawn: List<Double> = currentMap.data.getList("spawns.attacker")?.map {
            if(it !is Double) throw GameControllerException("Spawn locations do not contain exclusively doubles")
            it
        } ?: throw GameControllerException("Spawn set not found")

        val mainLocation = mainSpawn.unpackCoordinates(currentMap.world)
        val attackerLocation = attackerSpawn.unpackCoordinates(currentMap.world)

        suspendSync {
            gamePlayers.forEach {
                val location = if(it.tumblingPlayer.team == currentTeam) attackerLocation else mainLocation
                it.teleport(location)
            }
        }
    }

    /**
     * The method for the main gameplay loop for an individual game
     *
     * This should contain any kind of game-specific logic, and round handling if applicable
     */
    override suspend fun gameOn() {
        Bukkit.getOnlinePlayers().forEach {
            it.enableBossBar("countdownBossbar")
        }

        repeat(rounds) {
            currentRound++
            spawn(SpawnCycle.PRE_ROUND)
            asyncCountdown(10) {}
            preRound()
        }
    }

    suspend fun preRound() {
        delay(1000)
        val audience = Audience.audience(gamePlayers)
        val title = Title.title(
            Format.mm("<bold><yellow>Round $currentRound</yellow></bold>"),
            Format.mm("<team> are up!", Placeholder.component("team", currentTeam.formattedName)),
            Title.Times.times(Tick.of(5), Tick.of(80), Tick.of(5))
        )

        audience.showTitle(title)

        delay(4000)
        MiscUtils.subtitleCountdown(
            audience,
            Format.mm("<bold><yellow>Round $currentRound</yellow></bold>"),
            5
        )
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {
        delay(2000)
    }
}