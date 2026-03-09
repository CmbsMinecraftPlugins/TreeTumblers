package xyz.devcmb.tumblers.engine

import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.tumblingPlayer

/**
 * Base class for all games
 * @param id The unique identifier of the game
 * @param votable Whether this game is available for voting during the [GameController.State.VOTING] stage
 * @param maps A [Set] containing all the [xyz.devcmb.tumblers.engine.map.Map] instances
 * @param cutsceneSteps An [ArrayList] containing all the [CutsceneStep] instances
 *
 * @property currentState The current [State] of the individual game
 * @property loadedMaps An [ArrayList] containing all the [LoadedMap] instances
 * @property configRoot The root path for the games configuration
 * @property gamePlayers A [MutableSet] with all the players that were online when the game was started
 */
abstract class GameBase(
    val id: String,
    val votable: Boolean,
    val maps: Set<Map>,
    val cutsceneSteps: ArrayList<CutsceneStep>,
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

    @EventHandler
    fun playerMove(event: PlayerMoveEvent) {
        if(currentState == State.CUTSCENE) {
            event.isCancelled = true
        }
    }

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