package xyz.devcmb.tumblers.engine

import org.bukkit.World
import org.bukkit.event.Listener
import xyz.devcmb.tumblers.controllers.GameController

/**
 * Base class for all games
 * @param id The unique identifier of the game
 * @param votable Whether this game is available for voting during the [GameController.State.VOTING] stage
 * @param flags A set containing all the feature flags for this game
 * @param maps A [Set] containing all the [Map] instances
 */
abstract class GameBase(
    val id: String,
    val votable: Boolean,
    val flags: Set<Flag>,
    val maps: Set<Map>
): Listener {
    var currentState = State.UNLOADED

    open fun load(onComplete: (maps: HashMap<String, World>) -> Unit) {
        currentState = State.LOADING

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