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
import xyz.devcmb.tumblers.util.DebugUtil

@Controller("gameController", Controller.Priority.MEDIUM)
class GameController : IController {
    val games: ArrayList<GameBase> = ArrayList()
    var globalState: State = State.INACTIVE
    var activeGame: String? = null

    @Suppress("UNCHECKED_CAST")
    override fun init() {
        val reflections = Reflections(
            TreeTumblers::class.java.packageName,
            Scanners.TypesAnnotated
        )

        reflections.getTypesAnnotatedWith(EventGame::class.java)
            .filter { GameBase::class.java.isAssignableFrom(it) }
            .forEach { clazz ->
                val annotation = clazz.getAnnotation(EventGame::class.java)
                val controllerClass = clazz as Class<out GameBase>

                val instance: GameBase =
                    controllerClass.getDeclaredConstructor().newInstance()

                games.add(instance)
            }
    }

    fun startGame(id: String) {
        if(activeGame != null) throw GameOperatorException("Cannot start a game while one is currently active")
        val game = games.find { it.id == id }

        if(game == null) throw GameOperatorException("Cannot start a nonexistent game")

        activeGame = id
        Bukkit.getServer().pluginManager.registerEvents(game, TreeTumblers.plugin)

        TreeTumblers.pluginScope.launch {
            game.load()
            game.finishLoading()
            game.runCutscene()
        }
    }

    class Game(val id: String) {
        fun get(): GameBase {
            // this feels so bad
            val gameController = ControllerDelegate.getController("gameController") as GameController
            return gameController.games.find { it.id == id }!!
        }
    }

    enum class State {
        INACTIVE,
        VOTING,
        GAME_ACTIVE
    }
}

