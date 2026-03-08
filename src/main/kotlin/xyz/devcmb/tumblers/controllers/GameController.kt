package xyz.devcmb.tumblers.controllers

import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.GameOperatorException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.engine.GameBase

@Controller("gameController", Controller.Priority.MEDIUM)
class GameController : IController {
    val games: ArrayList<RegisteredGame> = ArrayList()
    var globalState: State = State.INACTIVE
    var activeGame: String? = null

    data class RegisteredGame(val id: String, val game: Class<out GameBase>)

    @Suppress("UNCHECKED_CAST")
    override fun init() {
        val reflections = Reflections(
            TreeTumblers::class.java.packageName,
            Scanners.TypesAnnotated
        )

        reflections.getTypesAnnotatedWith(EventGame::class.java)
            .filter { GameBase::class.java.isAssignableFrom(it) }
            .forEach { clazz ->
                val gameClass = clazz as Class<out GameBase>
                val gameId = gameClass.getDeclaredConstructor().newInstance().id

                games.add(RegisteredGame(gameId, gameClass))
            }
    }

    fun startGame(id: String) {
        if(activeGame != null) throw GameOperatorException("Cannot start a game while one is currently active")

        val gameClass = games.find { it.id == id }?.game
            ?: throw GameOperatorException("Cannot start a nonexistent game")

        val game = gameClass.getDeclaredConstructor().newInstance()

        activeGame = id
        Bukkit.getServer().pluginManager.registerEvents(game, TreeTumblers.plugin)

        TreeTumblers.pluginScope.launch {
            game.load()
            game.finishLoading()
            game.runCutscene()
            game.pregame()
        }
    }

    class Game(val id: String) {
        fun get(): GameBase {
            val gameController = ControllerDelegate.getController("gameController") as GameController
            val gameType = gameController.games.find { it.id == id }?.game
                ?: throw GameOperatorException("Cannot get a nonexistent game")

            return gameType.getDeclaredConstructor().newInstance()
        }
    }

    enum class State {
        INACTIVE,
        VOTING,
        GAME_ACTIVE
    }
}
