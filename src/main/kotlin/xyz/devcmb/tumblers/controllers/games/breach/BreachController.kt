package xyz.devcmb.tumblers.controllers.games.breach

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.EventController
import xyz.devcmb.tumblers.controllers.games.deathrun.DeathrunController.Companion.font
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.openHandledInventory
import xyz.devcmb.tumblers.util.validateLocation

@EventGame
class BreachController: GameBase(
    id = "breach",
    name = "Breach",
    votable = false,
    maps = setOf(
        Map("stadium")
    ),
    cutsceneSteps = arrayListOf(),
    flags = setOf(
        Flag.DISABLE_BLOCK_BREAKING
    ),
    icon = Component.text("\uEA00").font(font),
    scores = hashMapOf(),
    scoreboard = "breachScoreboard",
) {
    val currentMap: LoadedMap
        get() {
            return loadedMaps.getOrNull(currentRound - 1) ?: loadedMaps[0]
        }

    lateinit var playingTeams: Pair<Team, Team>
    val eventController by lazy {
        ControllerDelegate.getController("eventController") as EventController
    }
    val team1score: Int = 0
    val team2score: Int = 0
    val currentRound: Int = 1

    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    override suspend fun gameLoad() {
        val placements = eventController.getEventTeamPlacements()

        val team1 = placements.find { it.second == 1 }?.first ?: throw GameControllerException("No first place team found!")
        val team2 = placements.find {
            DebugUtil.info("${it.first.name}, ${it.second}")
            it.second == 2
        }?.first ?: throw GameControllerException("No second place team found!")

        playingTeams = Pair(team1, team2)

        loadMap(maps.random(), 1)
    }

    /**
     * The abstract method for spawning players in
     *
     * There was going to be some kind of system to do this automatically, but doing it manually seems to be a more flexible option, at least for now.
     *
     * @param cycle The stage where the players are spawned
     */
    override suspend fun spawn(cycle: SpawnCycle) {
        suspendSync {
            when(cycle) {
                SpawnCycle.PREGAME -> {}
                SpawnCycle.PRE_ROUND -> {
                    val team1spawn = currentMap.data.getList("team_1_spawn")?.validateLocation(currentMap.world)
                        ?: throw GameControllerException("Team 1 spawn not found")

                    val team2spawn = currentMap.data.getList("team_2_spawn")?.validateLocation(currentMap.world)
                        ?: throw GameControllerException("Team 2 spawn not found")

                    playingTeams.first.getOnlinePlayers().forEach {
                        it.teleport(team1spawn)
                        it.openHandledInventory("breachKitSelector")
                    }

                    playingTeams.second.getOnlinePlayers().forEach {
                        it.teleport(team2spawn)
                        it.openHandledInventory("breachKitSelector")
                    }
                }
            }
        }
    }

    /**
     * The method for the main gameplay loop for an individual game
     *
     * This should contain any kind of game-specific logic, and round handling if applicable
     */
    override suspend fun gameOn() {
        repeat(5) {
            spawn(SpawnCycle.PRE_ROUND)
            delay(10000)
        }
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {

    }

    fun giveWeapon(player: Player, kit: Material) {
        DebugUtil.info("i gave ${player.name} that sweet ${kit.name}")
    }

}