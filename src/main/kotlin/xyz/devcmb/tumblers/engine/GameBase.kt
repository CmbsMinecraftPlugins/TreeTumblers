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
import xyz.devcmb.tumblers.controllers.EventController
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.data.Team

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

    private val eventController: EventController by lazy {
        ControllerDelegate.getController("eventController") as EventController
    }

    open val debugToolkit: DebugToolkit? = null

    var countdownTime: Int = 0

    /**
     * The internal load stage called by the [GameController]
     */
    suspend fun load() {
        currentState = State.LOADING

        gamePlayers.addAll(Bukkit.getOnlinePlayers())
        gameParticipants.addAll(Bukkit.getOnlinePlayers().filter { it.tumblingPlayer?.team?.playingTeam == true })

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

    private var countdownJob: Job? = null
    private var countdownCancelled: Boolean = false

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

    fun asyncCountdown(time: Int, onComplete: (suspend () -> Unit)? = null) = TreeTumblers.pluginScope.launch {
        val success = countdown(time)
        if(onComplete != null && success) onComplete()
    }

    fun cancelCountdown() {
        countdownCancelled = true
        countdownJob?.cancel()
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
        DebugUtil.info("Granting $amount score to ${player.name} with source $source")
        eventController.grantScore(player, amount)
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