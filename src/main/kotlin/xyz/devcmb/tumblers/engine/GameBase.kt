package xyz.devcmb.tumblers.engine

import io.papermc.paper.util.Tick
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
import xyz.devcmb.tumblers.util.DebugUtil

/**
 * Base class for all games
 * @param id The unique identifier of the game
 * @param votable Whether this game is available for voting during the [GameController.State.VOTING] stage
 * @param flags A set containing all the feature flags for this game
 * @param maps A [Set] containing all the [Map] instances
 *
 * @property currentState The current [State] of the individual game
 * @property currentRound The current round
 */
abstract class GameBase(
    val id: String,
    val votable: Boolean,
    val flags: Set<Flag>,
    val maps: Set<Map>,
    val rounds: Int,
    val cutsceneSteps: ArrayList<CutsceneStep>
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
    var currentRound = 1

    val currentMap: LoadedMap?
        get() {
            return loadedMaps[currentRound]
        }

    val loadedMaps: ArrayList<LoadedMap> = ArrayList()
    val configRoot = "games.$id"

    val cutsceneObservers: MutableSet<Player> = HashSet()

    open suspend fun load() {
        currentState = State.LOADING

        Bukkit.getOnlinePlayers().forEach {
            val title = Title.title(
                Component.text("\uE000").font(NamespacedKey("tumbling", "hud")),
                Component.text("Loading...", NamedTextColor.AQUA),
                Title.Times.times(Tick.of(10), Tick.of(9999999), Tick.of(0))
            )

            it.showTitle(title)
        }

        var map = maps.random()
        for(i in 1..rounds) {
            if(flags.contains(Flag.RANDOMIZE_MAP_PER_ROUND)) {
                map = maps.random()
            }

            val loadedMap = map.load(i)
            DebugUtil.success("Loaded ${loadedMap.world.name} successfully!")
            loadedMaps.add(loadedMap)
        }
    }

    abstract suspend fun spawn()

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

    open suspend fun runCutscene() {
        currentState = State.CUTSCENE
        cutsceneObservers.addAll(Bukkit.getOnlinePlayers())

        cutsceneSteps.forEach {
            it.run(cutsceneObservers, currentMap!!)
        }
    }

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
        PRE_ROUND,
        ROUND_ON,
        POST_ROUND,
        POST_GAME
    }
}