package xyz.devcmb.tumblers.engine

import io.papermc.paper.util.Tick
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.controllers.EventController
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.unpackCoordinates

/**
 * Base class for all games
 * @param id The unique identifier of the game
 * @param votable Whether this game is available for voting during the voting stage
 * @param maps A [Set] containing all the [xyz.devcmb.tumblers.engine.map.Map] instances
 * @param cutsceneSteps An [ArrayList] containing all the [CutsceneStep] instances
 * @param flags A [Set] of [Flag] enums to determine certain shared behaviors
 * @param scores A [HashMap] of [ScoreSource]s to the amount of score they give
 *
 * @property currentState The current [State] of the individual game
 * @property loadedMaps An [ArrayList] containing all the [LoadedMap] instances
 * @property configRoot The root path for the games configuration
 * @property gamePlayers A [MutableSet] with all the players that were online when the game was started
 * @property gameParticipants a [MutableSet] with all the players that were online when the game was started that are on a team labeled [Team.playingTeam]
 * @property debugToolkit An optional instance of a [DebugToolkit] for certain developer commands (you really should fill this out, but you can be lazy if you really don't want to)
 */
abstract class GameBase(
    val id: String,
    val votable: Boolean,
    val maps: Set<Map>,
    val cutsceneSteps: ArrayList<CutsceneStep>,
    val flags: Set<Flag>,
    val scores: HashMap<ScoreSource, Int>
): Listener {
    init {
        maps.forEach {
            it.init(this)
        }
    }

    var currentState = State.UNLOADED
        set(value) {
            DebugUtil.info("Transitioning to GameState ${value.name}")
            field = value
        }

    val loadedMaps: ArrayList<LoadedMap> = ArrayList()
    val configRoot = "games.$id"

    val gamePlayers: MutableSet<Player> = HashSet()
    val gameParticipants: MutableSet<Player> = HashSet()

    val teamScores: HashMap<Team, Int> = HashMap()
    val playerScores: HashMap<Player, Int> = HashMap()

    private val eventController: EventController by lazy {
        ControllerDelegate.getController("eventController") as EventController
    }

    open val debugToolkit: DebugToolkit? = null

    var countdownTime: Int = 0

    companion object {
        @Configurable("lobby.world")
        var lobbyWorld: String = "world"

        @Configurable("lobby.position")
        var lobbyPosition: List<Double> = listOf(0.0,78.0,0.0)
    }

    /**
     * The internal load stage called by the [GameController]
     */
    suspend fun load() {
        currentState = State.LOADING

        gamePlayers.addAll(Bukkit.getOnlinePlayers())
        gameParticipants.addAll(Bukkit.getOnlinePlayers().filter { it.tumblingPlayer.team.playingTeam })
        gameParticipants.forEach {
            playerScores.put(it, 0)
        }
        Team.entries.filter { it.playingTeam }.forEach {
            teamScores.put(it, 0)
        }

        Bukkit.getOnlinePlayers().forEach {
            val title = Title.title(
                Component.text("\uE000").font(NamespacedKey("tumbling", "hud")),
                Component.text("Loading...", NamedTextColor.AQUA),
                Title.Times.times(Tick.of(10), Tick.of(9999999), Tick.of(0))
            )

            it.showTitle(title)
        }

        gameLoad()
    }

    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    abstract suspend fun gameLoad()

    /**
     * The method that is invoked once the coroutine stops yielding
     *
     * Can be overridden if necessary, but I doubt it's necessary
     */
    open suspend fun finishLoading() {
        Bukkit.getOnlinePlayers().forEach {
            val title = Title.title(
                Component.text("\uE000").font(NamespacedKey("tumbling", "hud")),
                Component.text("Loading...", NamedTextColor.AQUA),
                Title.Times.times(Tick.of(0), Tick.of(10), Tick.of(40))
            )

            it.showTitle(title)
        }
    }

    /**
     * A utility method for loading maps into the [loadedMaps] using [Map.load]
     * @param map The map instance to load
     * @param index The index of the map to be formated as `world_name-index`
     * @return A [LoadedMap] created from the [map]
     */
    suspend fun loadMap(map: Map, index: Int): LoadedMap {
        val loadedMap = map.load(index)
        loadedMaps.add(loadedMap)

        DebugUtil.success("Loaded ${loadedMap.world.name} successfully!")
        return loadedMap
    }

    /**
     * The sequence for running [CutsceneStep] instances
     */
    open suspend fun runCutscene() {
        currentState = State.CUTSCENE

        cutsceneSteps.forEach {
            it.run(gamePlayers, loadedMaps.first())
            it.cleanup()
        }
    }

    /**
     * The internal method for pre-pregame invoked by the [GameController]
     */
    suspend fun pregame() {
        currentState = State.PREGAME
        spawn(SpawnCycle.PREGAME)
        gamePregame()
    }

    /**
     * The main method for the pregame state
     *
     * By default, all this does is wait 2 seconds and continue with execution
     *
     * This is where any player configurable things should be done (kit selection, settings, etc.)
     */
    open suspend fun gamePregame() {
        delay(2000)
    }

    /**
     * The abstract method for spawning players in
     *
     * There was going to be some kind of system to do this automatically, but doing it manually seems to be a more flexible option, at least for now.
     *
     * @param cycle The stage where the players are spawned
     */
    abstract suspend fun spawn(cycle: SpawnCycle)

    /**
     * Internal function for starting the game
     */
    suspend fun gameMain() {
        currentState = State.GAME_ON
        gameOn()
    }

    /**
     * The method for the main gameplay loop for an individual game
     *
     * This should contain any kind of game-specific logic, and round handling if applicable
     */
    abstract suspend fun gameOn()

    /**
     * The method to invoke after the game has ended
     */
    abstract suspend fun postGame()

    /**
     * The method for cleaning up anything created during the game
     *
     * This should be expanded upon if the game has any listeners registered not in the main class.
     */
    open suspend fun cleanup() {
        suspendSync {
            Bukkit.getOnlinePlayers().forEach {
                it.inventory.clear()
                it.teleport(lobbyPosition.unpackCoordinates(Bukkit.getWorld(lobbyWorld)!!))
            }
        }

        loadedMaps.forEach {
            it.cleanup()
        }
    }

    private var countdownJob: Job? = null
    private var countdownCancelled: Boolean = false

    /**
     * Perform a countdown synchronously, stored in the [countdownTime] field
     * @param time How long to run the countdown for
     * @return Whether the timer fully completed or was abruptly ended
     */
    suspend fun countdown(time: Int): Boolean {
        countdownJob?.cancel()
        countdownJob = TreeTumblers.pluginScope.launch {
            countdownTime = time
            while(true) {
                delay(1000)
                if (countdownTime <= 0) return@launch
                countdownTime--
            }
        }
        countdownJob!!.join()
        val result = !countdownCancelled
        countdownCancelled = false
        return result
    }

    /**
     * Runs the [countdown] method asynchronously
     * @param time How long to run the countdown for
     * @param onComplete The function to invoke when/if the countdown fully finishes
     */
    fun asyncCountdown(time: Int, onComplete: (suspend () -> Unit)? = null) = TreeTumblers.pluginScope.launch {
        val success = countdown(time)
        if(onComplete != null && success) onComplete()
    }

    /**
     * Cancel a countdown if one is active
     */
    fun cancelCountdown() {
        if(countdownJob == null) return

        countdownCancelled = true
        countdownJob!!.cancel()
    }

    /**
     * Gets a score amount from the provided [scores] table
     * @param source The source to get
     * @return The amount of score a source gives (or 0 if not specified)
     */
    fun getScoreSource(source: ScoreSource): Int {
        return scores[source] ?: 0
    }

    /**
     * Grants score to a [Player] and their team
     * @param player The player to give score to
     * @param source The source of score
     */
    fun grantScore(player: Player, source: ScoreSource) {
        val amount = getScoreSource(source)
        val team = player.tumblingPlayer.team
        teamScores.put(team, teamScores[team]!! + amount)
        playerScores.put(player, (playerScores[player] ?: 0) + amount)
        DebugUtil.info("Granting $amount score to ${player.name} with source $source")
        eventController.grantScore(player, amount)
    }

    /**
     * Get the current placements for all playing teams
     * @return An ArrayList of teams in order of placement
     */
    fun getTeamPlacements(): Set<Pair<Team, Int>> {
        // ChatGPT generated code
        val sorted = teamScores.entries.sortedByDescending { it.value }

        val rankedWithTies = mutableSetOf<Pair<Team, Int>>()

        var currentPlace = 0
        var lastScore: Int? = null
        var index = 0

        for ((team, score) in sorted) {
            index++

            if (score != lastScore) {
                currentPlace = index
                lastScore = score
            }

            rankedWithTies.add(team to currentPlace)
        }

        return rankedWithTies
    }

    @EventHandler
    fun playerDismountEvent(event: VehicleExitEvent) {
        val player = event.exited
        if(player !is Player || currentState != State.CUTSCENE) return

        event.isCancelled = true
    }

    @EventHandler
    fun playerDeathEvent(event: PlayerDeathEvent){
        if(flags.contains(Flag.ENABLE_ITEM_DROPS)) return
        event.drops.clear()
    }

    /**
     * The current state of the active game
     */
    enum class State {
        UNLOADED,
        LOADING,
        CUTSCENE,
        PREGAME,
        GAME_ON,
        POST_GAME
    }

    enum class SpawnCycle {
        PREGAME,
        PRE_ROUND
    }
}