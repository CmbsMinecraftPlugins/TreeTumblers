package xyz.devcmb.tumblers.controllers

import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.GameOperatorException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.util.hunger

@Controller("gameController", Controller.Priority.MEDIUM)
class GameController : IController {
    val games: ArrayList<RegisteredGame> = ArrayList()
    var activeGame: GameBase? = null

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

        activeGame = game
        Bukkit.getServer().pluginManager.registerEvents(game, TreeTumblers.plugin)

        TreeTumblers.pluginScope.launch {
            game.load()
            game.finishLoading()
            game.runCutscene()
            game.pregame()
            game.gameMain()
            game.postGame()
            game.cleanup()

            HandlerList.unregisterAll(game)
            activeGame = null
        }
    }

    @EventHandler
    fun playerFoodEvent(event: FoodLevelChangeEvent) {
        if(activeGame == null || !activeGame!!.flags.contains(Flag.ENABLE_HUNGER)) event.isCancelled = true
    }

    @EventHandler
    fun playerDamageEvent(event: EntityDamageEvent) {
        if(event.entity !is Player || activeGame != null) return
        event.isCancelled = true
    }

    @EventHandler
    fun playerJoin(event: PlayerJoinEvent) {
        if(activeGame == null || !activeGame!!.flags.contains(Flag.ENABLE_HUNGER)) {
            val player = event.player
            player.foodLevel = 20
            player.saturation = 0f
            player.hunger()
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
}
