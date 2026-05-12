package xyz.devcmb.tumblers.controllers.games

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import xyz.devcmb.tumblers.ControllerRegistry
import xyz.devcmb.tumblers.GameOperatorException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.ControllerBase
import xyz.devcmb.tumblers.controllers.event.BadgeController
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.util.hunger

@Controller(Controller.Priority.HIGH)
class GameController : ControllerBase() {
    val games: ArrayList<RegisteredGame> = ArrayList()
    var activeGame: GameBase? = null
    var activeGameJob: Job? = null

    data class RegisteredGame(
        val id: String,
        val name: String,
        val votable: Boolean,
        val game: Class<out GameBase>,
        val logo: Component,
        val badges: List<BadgeController.Badge>?
    )

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
                val templateInstance = gameClass.getDeclaredConstructor().newInstance()

                games.add(RegisteredGame(
                    templateInstance.id,
                    templateInstance.name,
                    templateInstance.votable,
                    gameClass,
                    templateInstance.logo,
                    templateInstance.badges
                ))
            }
    }

    fun startGameAsync(id: String) {
        TreeTumblers.Companion.pluginScope.launch {
            startGame(id)
        }
    }

    suspend fun startGame(id: String) {
        if(activeGame != null) throw GameOperatorException("Cannot start a game while one is currently active")

        val gameClass = games.find { it.id == id }?.game
            ?: throw GameOperatorException("Cannot start a nonexistent game")

        val game = gameClass.getDeclaredConstructor().newInstance()

        activeGame = game
        Bukkit.getServer().pluginManager.registerEvents(game, TreeTumblers.Companion.plugin)

        game.load()
        game.finishLoading()
        game.runCutscene()
        game.pregame()
        activeGameJob = TreeTumblers.Companion.pluginScope.launch {
            game.gameMain()
        }
        activeGameJob!!.join()
        activeGameJob = null
        game.basePostGame()
        TreeTumblers.Companion.pluginScope.launch {
            game.cleanup()
        }
        HandlerList.unregisterAll(game)
        activeGame = null
    }

    @EventHandler
    fun playerFoodEvent(event: FoodLevelChangeEvent) {
        if(activeGame == null || !activeGame!!.flags.contains(Flag.ENABLE_HUNGER)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun playerJoin(event: PlayerJoinEvent) {
        if(activeGame == null || !activeGame!!.flags.contains(Flag.ENABLE_HUNGER)) {
            val player = event.player
            player.foodLevel = 20
            player.saturation = 0f
            player.hunger()
        }
    }

    class Game(val id: String) {
        fun getTemplate(): GameBase {
            val gameController = ControllerRegistry.getController<GameController>()
            val gameType = gameController.games.find { it.id == id }?.game
                ?: throw GameOperatorException("Cannot get a nonexistent game")

            return gameType.getDeclaredConstructor().newInstance()
        }
    }
}