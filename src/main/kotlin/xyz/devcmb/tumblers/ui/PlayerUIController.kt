package xyz.devcmb.tumblers.ui

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.Objective
import xyz.devcmb.tumblers.Constants
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.EventController
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.ui.bossbar.CountdownBossbar
import xyz.devcmb.tumblers.ui.bossbar.DebugBossbar
import xyz.devcmb.tumblers.ui.bossbar.HandledBossbar
import xyz.devcmb.tumblers.ui.bossbar.games.crumble.AliveTeamsBossbar
import xyz.devcmb.tumblers.ui.bossbar.games.deathrun.CooldownBossbar
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.ui.inventory.ReadyCheckInventory
import xyz.devcmb.tumblers.ui.inventory.breach.BreachKitSelector
import xyz.devcmb.tumblers.ui.inventory.crumble.CrumbleKitSelector
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.ui.scoreboard.IntermissionScoreboard
import xyz.devcmb.tumblers.ui.scoreboard.games.BreachScoreboard
import xyz.devcmb.tumblers.ui.scoreboard.games.CrumbleScoreboard
import xyz.devcmb.tumblers.ui.scoreboard.games.DeathrunScoreboard
import xyz.devcmb.tumblers.ui.scoreboard.games.SnifferCaretakerScoreboard
import xyz.devcmb.tumblers.util.runTaskTimer

class PlayerUIController(val player: Player) {
    val inventories: ArrayList<HandledInventory> = ArrayList()
    val bossBars: ArrayList<HandledBossbar> = ArrayList()
    val scoreboards: ArrayList<HandledScoreboard> = ArrayList()

    var activeScoreboards: ArrayList<String> = ArrayList()
    var activeScoreboardObjectives: HashMap<String, Set<Objective>> = HashMap()

    val activeBossBars: HashMap<String, BossBar> = HashMap()
    val paddingBossBars: HashMap<String, ArrayList<BossBar>> = HashMap()
    val gameController = ControllerDelegate.getController("gameController") as GameController
    val eventController = ControllerDelegate.getController("eventController") as EventController

    val playerScoreboard = Bukkit.getScoreboardManager().newScoreboard
    val updateTask: BukkitTask

    init {
        registerInventory(CrumbleKitSelector(player, gameController))
        registerInventory(BreachKitSelector(player, gameController))
        registerInventory(ReadyCheckInventory(player, eventController))

        registerBossBar(AliveTeamsBossbar(gameController))
        registerBossBar(CountdownBossbar(gameController))
        registerBossBar(CooldownBossbar(player, gameController))
        registerBossBar(DebugBossbar())

        if(Constants.IS_DEVELOPMENT) {
            enableBossBar("debugBossbar")
        }

        registerScoreboard(CrumbleScoreboard(gameController, player))
        registerScoreboard(SnifferCaretakerScoreboard(gameController, player))
        registerScoreboard(DeathrunScoreboard(gameController, player))
        registerScoreboard(BreachScoreboard())

        registerScoreboard(IntermissionScoreboard(eventController, player))

        if(gameController.activeGame == null) {
            activateScoreboard("intermissionScoreboard")
        }

        player.scoreboard = playerScoreboard

        updateTask = runTaskTimer(0, 5) {
            bossBars.forEach {
                if(activeBossBars.containsKey(it.id)) {
                    val bar = activeBossBars[it.id]!!
                    bar.name(it.getComponent())
                }
            }

            playerScoreboard.objectives.forEach {
                it.unregister()
            }

            activeScoreboards.forEach { id ->
                val scoreboard = scoreboards.find { it.id == id }!!
                scoreboard.getObjectives(playerScoreboard)
            }
        }
    }

    fun cleanup() {
        updateTask.cancel()
    }

    fun registerInventory(inv: HandledInventory) {
        inventories.add(inv)
    }

    fun registerBossBar(bar: HandledBossbar) {
        bossBars.add(bar)
    }

    fun registerScoreboard(board: HandledScoreboard) {
        scoreboards.add(board)
    }

    fun openInventory(id: String) {
        val handledInventory = inventories.find { it.id == id }
        if(handledInventory == null) throw IllegalArgumentException("Inventory with an id of $id does not exist")

        handledInventory.inventory.show()
    }

    fun enableBossBar(id: String) {
        val bar = bossBars.find { it.id == id }
        require(bar != null)

        if(activeBossBars.containsKey(id)) return

        val bossbar = BossBar.bossBar(
            bar.getComponent(),
            0f,
            BossBar.Color.WHITE,
            BossBar.Overlay.PROGRESS
        )

        player.showBossBar(bossbar)

        activeBossBars.put(id, bossbar)
        paddingBossBars.put(id, ArrayList())

        repeat(bar.padding) {
            val paddingBar = BossBar.bossBar(
                Component.empty(),
                0f,
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS
            )
            player.showBossBar(paddingBar)
            paddingBossBars[id]!!.add(paddingBar)
        }
    }

    fun disableBossBar(id: String) {
        val bar = bossBars.find { it.id == id }
        require(bar != null)

        if(!activeBossBars.containsKey(id)) return

        val activeBar = activeBossBars[id]!!
        activeBar.viewers().forEach {
            activeBar.removeViewer(it as Audience)
        }

        val paddingBars = paddingBossBars[id]!!
        paddingBars.forEach { bar ->
            bar.viewers().forEach {
                bar.removeViewer(it as Audience)
            }
        }

        activeBossBars.remove(id)
        paddingBossBars.remove(id)
    }

    fun activateScoreboard(id: String) {
        val scoreboard = scoreboards.find { it.id == id }
        if(scoreboard == null) throw IllegalArgumentException("Unknown Scoreboard ID: $id")

        if(activeScoreboards.contains(id)) return

        activeScoreboards.add(id)
    }

    fun deactivateScoreboard(id: String) {
        val scoreboard = scoreboards.find { it.id == id }
        if(scoreboard == null) throw IllegalArgumentException("Unknown Scoreboard ID: $id")

        if(!activeScoreboards.contains(id)) return
        activeScoreboards.remove(id)

        val activeObjectives = activeScoreboardObjectives[id] ?: arrayListOf()
        activeObjectives.forEach {
            it.unregister()
        }
    }
}