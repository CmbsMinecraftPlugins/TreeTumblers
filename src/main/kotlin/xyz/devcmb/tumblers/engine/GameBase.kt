package xyz.devcmb.tumblers.engine

import kotlinx.coroutines.*
import org.bukkit.World
import org.bukkit.event.Listener
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.GameController
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
    val rounds: Int
): Listener {
    init {
        maps.forEach {
            it.init(this)
        }
    }

    var currentState = State.UNLOADED
    var currentRound = 1
    val loadedMaps: ArrayList<LoadedMap> = ArrayList()
    val configRoot = "games.$id"

    open suspend fun load() {
        currentState = State.LOADING
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