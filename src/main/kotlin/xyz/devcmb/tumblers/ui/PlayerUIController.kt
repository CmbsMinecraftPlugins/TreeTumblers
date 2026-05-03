package xyz.devcmb.tumblers.ui

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Team
import xyz.devcmb.tumblers.Constants
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.EventController
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.ui.bossbar.CountdownBossbar
import xyz.devcmb.tumblers.ui.bossbar.DebugBossbar
import xyz.devcmb.tumblers.ui.bossbar.HandledBossbar
import xyz.devcmb.tumblers.ui.bossbar.games.breach.ScoreBossbar
import xyz.devcmb.tumblers.ui.bossbar.games.crumble.AliveTeamsBossbar
import xyz.devcmb.tumblers.ui.bossbar.games.deathrun.CooldownBossbar
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.ui.inventory.ReadyCheckInventory
import xyz.devcmb.tumblers.ui.inventory.SpectateInventory
import xyz.devcmb.tumblers.ui.inventory.breach.BreachKitSelector
import xyz.devcmb.tumblers.ui.inventory.crumble.CrumbleKitSelector
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.ui.scoreboard.HealthIndicatorScoreboard
import xyz.devcmb.tumblers.ui.scoreboard.IntermissionScoreboard
import xyz.devcmb.tumblers.ui.scoreboard.games.BreachScoreboard
import xyz.devcmb.tumblers.ui.scoreboard.games.CrumbleScoreboard
import xyz.devcmb.tumblers.ui.scoreboard.games.DeathrunScoreboard
import xyz.devcmb.tumblers.ui.scoreboard.games.PartyScoreboard
import xyz.devcmb.tumblers.ui.scoreboard.games.SnifferCaretakerScoreboard
import xyz.devcmb.tumblers.util.runTaskTimer
import xyz.devcmb.tumblers.util.tumblingPlayer

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

    val playerTeam: Team
    val otherTeams: HashMap<xyz.devcmb.tumblers.data.Team, Team> = HashMap()

    init {
        registerInventories()
        registerBossBars()
        registerScoreboards()

        val playerTumblingTeam = player.tumblingPlayer.team
        playerTeam = playerScoreboard.registerNewTeam("playerTeam")
        playerTeam.prefix(Component.text(" ").append(playerTumblingTeam.formattedIcon).append(Component.text(" ")))
        playerTeam.color(playerTumblingTeam.namedColor)
        playerTeam.suffix(Component.text(" "))
        // just sent always because teams are local anyways
        // i love not having to worry about 2 environments
        // unlike SOME OTHER PLATFORM
        playerTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)

        xyz.devcmb.tumblers.data.Team.entries.forEach {
            val team = playerScoreboard.registerNewTeam(it.name.lowercase())
            team.color(it.namedColor)
            team.prefix(Component.text(" ").append(it.formattedIcon).append(Component.text(" ")))
            team.suffix(Component.text(" "))
            otherTeams.put(it, team)
        }

        Bukkit.getOnlinePlayers().forEach {
            if(it.tumblingPlayer.team == player.tumblingPlayer.team) {
                playerTeam.addEntry(it.name)
            } else {
                otherTeams[it.tumblingPlayer.team]!!.addEntry(it.name)
            }
        }

        player.scoreboard = playerScoreboard

        updateTask = runTaskTimer(0, 5) {
            bossBars.forEach {
                if(activeBossBars.containsKey(it.id)) {
                    val bar = activeBossBars[it.id]!!
                    bar.name(it.getComponent())
                }
            }

            activeScoreboards.forEach { id ->
                val scoreboard = scoreboards.find { it.id == id }!!
                scoreboard.update(playerScoreboard)
            }
        }
    }

    fun playerJoin(plr: Player) {
        if(plr.tumblingPlayer.team == player.tumblingPlayer.team) {
            playerTeam.addEntry(plr.name)
        } else {
            otherTeams[plr.tumblingPlayer.team]!!.addEntry(plr.name)
        }
    }

    fun playerLeave(plr: Player) {
        playerTeam.removeEntry(plr.name)
        otherTeams[plr.tumblingPlayer.team]!!.removeEntry(plr.name)
    }

    fun registerInventories() {
        registerInventory(CrumbleKitSelector(player, gameController))
        registerInventory(BreachKitSelector(player, gameController))
        registerInventory(ReadyCheckInventory(player, eventController))
        registerInventory(SpectateInventory(player, gameController))
    }

    fun registerBossBars() {
        registerBossBar(AliveTeamsBossbar(gameController))
        registerBossBar(CountdownBossbar(gameController))
        registerBossBar(CooldownBossbar(player, gameController))
        registerBossBar(ScoreBossbar(gameController))
        registerBossBar(DebugBossbar())

        if(Constants.IS_DEVELOPMENT) {
            enableBossBar("debugBossbar")
        }
    }

    fun registerScoreboards() {
        registerScoreboard(CrumbleScoreboard(gameController, player))
        registerScoreboard(SnifferCaretakerScoreboard(gameController, player))
        registerScoreboard(DeathrunScoreboard(gameController, player))
        registerScoreboard(PartyScoreboard(gameController, player))
        registerScoreboard(BreachScoreboard(gameController, player))

        registerScoreboard(IntermissionScoreboard(eventController, player))
        registerScoreboard(HealthIndicatorScoreboard(player))

        if(gameController.activeGame == null) {
            activateScoreboard("intermissionScoreboard")
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

        scoreboard.enable(playerScoreboard)
        activeScoreboards.add(id)
    }

    fun deactivateScoreboard(id: String) {
        val scoreboard = scoreboards.find { it.id == id }
        if(scoreboard == null) throw IllegalArgumentException("Unknown Scoreboard ID: $id")

        if(!activeScoreboards.contains(id)) return
        activeScoreboards.remove(id)
        scoreboard.disable(playerScoreboard)

        val activeObjectives = activeScoreboardObjectives[id] ?: arrayListOf()
        activeObjectives.forEach {
            it.unregister()
        }
    }
}