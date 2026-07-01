package xyz.devcmb.tumblers.controllers.games

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import xyz.devcmb.tumblers.GameOperatorException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.IController
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.base.AbstractGame
import xyz.devcmb.tumblers.util.hunger

@Controller(Controller.Priority.HIGH)
object GameController : IController {
    val games: ArrayList<RegisteredGame> = ArrayList()
    var activeGame: AbstractGame? = null
    var activeGameJob: Job? = null

    data class RegisteredGame(
        val game: Class<out AbstractGame>,
        val data: GameData,
    ) {
        fun getTemplate(): AbstractGame {
            val gameType = games.find { it.data.id == data.id }?.game
                ?: throw GameOperatorException("Cannot get a nonexistent game")

            return gameType.getDeclaredConstructor().newInstance()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun init() {
        val reflections = Reflections(
            TreeTumblers::class.java.packageName,
            Scanners.TypesAnnotated
        )

        reflections.getTypesAnnotatedWith(EventGame::class.java)
            .filter { AbstractGame::class.java.isAssignableFrom(it) }
            .forEach { clazz ->
                val gameClass = clazz as Class<out AbstractGame>
                val templateInstance = gameClass.getDeclaredConstructor().newInstance()

                games.add(RegisteredGame(
                    gameClass,
                    templateInstance.data
                ))
            }
    }

    fun startGameAsync(id: String) {
        TreeTumblers.pluginScope.launch {
            startGame(id)
        }
    }

    suspend fun startGame(id: String) {
        if(activeGame != null) throw GameOperatorException("Cannot start a game while one is currently active")

        val gameClass = games.find { it.data.id == id }?.game
            ?: throw GameOperatorException("Cannot start a nonexistent game")

        val game = gameClass.getDeclaredConstructor().newInstance()

        activeGame = game
        Bukkit.getServer().pluginManager.registerEvents(game, TreeTumblers.plugin)

        game.load(true)
        game.finishLoading()
        game.runCutscene()
        game.pregame()
        activeGameJob = TreeTumblers.pluginScope.launch {
            game.gameMain()
        }
        activeGameJob!!.join()
        activeGameJob = null
        game.basePostGame()
        TreeTumblers.pluginScope.launch {
            game.cleanup()
        }
        HandlerList.unregisterAll(game)
        activeGame = null
    }

    @EventHandler
    fun playerFoodEvent(event: FoodLevelChangeEvent) {
        if(activeGame == null || !activeGame!!.data.flags.contains(Flag.ENABLE_HUNGER)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun playerJoin(event: PlayerJoinEvent) {
        if(activeGame == null || !activeGame!!.data.flags.contains(Flag.ENABLE_HUNGER)) {
            val player = event.player
            player.foodLevel = 20
            player.saturation = 0f
            player.hunger()
        }
    }
}