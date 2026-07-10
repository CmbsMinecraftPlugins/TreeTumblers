package xyz.devcmb.tumblers.ui

import com.noxcrew.interfaces.view.InterfaceView
import kotlinx.coroutines.launch
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Team
import xyz.devcmb.tumblers.Constants
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.player.UIController
import xyz.devcmb.tumblers.ui.actionbar.*
import xyz.devcmb.tumblers.ui.actionbar.games.*
import xyz.devcmb.tumblers.ui.bossbar.*
import xyz.devcmb.tumblers.ui.bossbar.games.breach.ScoreBossbar
import xyz.devcmb.tumblers.ui.bossbar.games.crumble.CrumbleBossbar
import xyz.devcmb.tumblers.ui.bossbar.games.deathrun.CooldownBossbar
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.ui.inventory.brawl.BrawlKitSelector
import xyz.devcmb.tumblers.ui.inventory.event.ReadyCheckInventory
import xyz.devcmb.tumblers.ui.inventory.breach.BreachKitSelector
import xyz.devcmb.tumblers.ui.inventory.crumble.CrumbleKitSelector
import xyz.devcmb.tumblers.ui.inventory.hub.*
import xyz.devcmb.tumblers.ui.inventory.spectate.CrumbleSpectateMenu
import xyz.devcmb.tumblers.ui.inventory.spectate.GlobalSpectateInventory
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.ui.scoreboard.*
import xyz.devcmb.tumblers.util.runTaskTimer
import xyz.devcmb.tumblers.util.tumblingPlayer
import kotlin.reflect.full.primaryConstructor

class PlayerUIController(val player: Player) {
    val inventories: ArrayList<HandledInventory> = ArrayList()
    val bossBars: ArrayList<HandledBossbar> = ArrayList()
    val scoreboards: ArrayList<HandledScoreboard> = ArrayList()
    val actionbars: ArrayList<HandledActionBar> = ArrayList()

    val activeScoreboards: MutableSet<String> = HashSet()
    val activeScoreboardObjectives: HashMap<String, Set<Objective>> = HashMap()
    val activeActionBars: MutableSet<String> = HashSet()

    val activeBossBars: HashMap<String, BossBar> = HashMap()
    val paddingBossBars: HashMap<String, ArrayList<BossBar>> = HashMap()

    val playerScoreboard = Bukkit.getScoreboardManager().newScoreboard
    val updateTask: BukkitTask

    val playerTeam: Team
    val otherTeams: HashMap<xyz.devcmb.tumblers.data.Team, Team> = HashMap()

    var currentInventory: Pair<InterfaceView, String>? = null

    init {
        registerInventories()
        registerBossBars()
        registerScoreboards()
        registerActionBars()

        val playerTumblingTeam = player.tumblingPlayer.team
        playerTeam = playerScoreboard.registerNewTeam("playerTeam")
        playerTeam.color(playerTumblingTeam.namedColor)
        playerTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)
        playerTeam.setCanSeeFriendlyInvisibles(true)

        xyz.devcmb.tumblers.data.Team.entries.forEach {
            if(it == player.tumblingPlayer.team) return@forEach

            val team = playerScoreboard.registerNewTeam(it.name.lowercase())
            team.color(it.namedColor)
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)
            otherTeams[it] = team
        }

        Bukkit.getOnlinePlayers().forEach {
            if(it.tumblingPlayer.team == player.tumblingPlayer.team) {
                playerTeam.addEntry(it.name)
            } else {
                otherTeams[it.tumblingPlayer.team]!!.addEntry(it.name)
            }
        }

        player.scoreboard = playerScoreboard

        var lastActionBar: Component? = null
        var lastSentActionBar: Long = 0
        val currentBossbarNames: HashMap<String, Component> = HashMap()
        var lastSentBossbar: Long = 0
        updateTask = runTaskTimer(0, 1) {
            val currentTime = System.nanoTime()

            val elapsedNanosBossbar = currentTime - lastSentBossbar
            val elapsedMillisBossbar = elapsedNanosBossbar / 1_000_000
            bossBars.forEach {
                if(activeBossBars.containsKey(it.id)) {
                    val bar = activeBossBars[it.id]!!
                    val newComponent = it.getComponent()
                    if(newComponent != (currentBossbarNames[it.id] ?: Component.empty()) || elapsedMillisBossbar > 3000) {
                        bar.name(newComponent)

                        currentBossbarNames[it.id] = newComponent
                        lastSentBossbar = System.nanoTime()
                    }
                }
            }

            activeScoreboards.toList().forEach { id ->
                val scoreboard = scoreboards.find { it.id == id }!!
                scoreboard.update(playerScoreboard)
            }

            val bar = UIController.tinsel.draw(255, Style.empty()) {
                activeActionBars.toList().forEach { id ->
                    var (cX, cY) = it.cursorX() to it.cursorY()

                    val actionbar = actionbars.find { entry -> entry.id == id }!!
                    actionbar.draw(it)

                    it.moveCursor(cX, cY)
                }
            }

            val elapsedNanosActionbar = currentTime - lastSentActionBar
            val elapsedMillisActionbar = elapsedNanosActionbar / 1_000_000
            if(bar != lastActionBar || elapsedMillisActionbar > 1000) {
                player.sendActionBar(bar)
                lastSentActionBar = currentTime
            }
            lastActionBar = bar
        }

        player.tumblingPlayer.currentScoreboards.forEach(this::activateScoreboard)
        player.tumblingPlayer.currentBossbars.forEach(this::enableBossBar)
        player.tumblingPlayer.currentActionBars.forEach(this::enableActionBar)
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
        registerInventory(CrumbleKitSelector())
        registerInventory(BreachKitSelector())
        registerInventory(ReadyCheckInventory())
        registerInventory(HubNavigationInventory())
        registerInventory(BadgeCollectionInventory())
        registerInventory(BrawlKitSelector())

        registerInventory(GlobalSpectateInventory())
        registerInventory(CrumbleSpectateMenu())
    }

    fun registerBossBars() {
        registerBossBar(CrumbleBossbar(player))
        registerBossBar(CountdownBossbar())
        registerBossBar(CooldownBossbar(player))
        registerBossBar(ScoreBossbar())
        registerBossBar(DebugBossbar())

        if(Constants.IS_DEVELOPMENT) {
            enableBossBar("debugBossbar")
        }
    }

    fun registerScoreboards() {
        GameController.games.forEach {
            val scoreboard = it.data.scoreboard.primaryConstructor!!.call(player, it.data)
            registerScoreboard(scoreboard)
        }

        registerScoreboard(IntermissionScoreboard(player))

        if(GameController.activeGame == null) {
            activateScoreboard("intermission_scoreboard")
        }
    }

    fun registerActionBars() {
        registerActionBar(SpectatorActionBar())
        registerActionBar(VotingDisplayActionBar(player))
        registerActionBar(PreEventActionBar())
        registerActionBar(WaitingForPlayersActionBar())
        registerActionBar(CrumbleActionBar(player))
        registerActionBar(DeathrunActionBar(player))
        registerActionBar(BrawlActionBar(player))
        registerActionBar(PartyActionBar(player))
        registerActionBar(FloodEscapeActionBar(player))
        registerActionBar(BreachActionBar(player))
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

    fun registerActionBar(bar: HandledActionBar) {
        actionbars.add(bar)
    }

    fun openInventory(id: String) {
        val handledInventory = inventories.find { it.id == id }
        if(handledInventory == null) throw IllegalArgumentException("Inventory with an id of $id does not exist")

        TreeTumblers.pluginScope.launch {
            val pane = handledInventory.inventory.open(player)
            currentInventory = Pair(pane, id)
        }
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

        activeBossBars[id] = bossbar
        paddingBossBars[id] = ArrayList()

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

    fun enableActionBar(id: String) {
        val actionbar = actionbars.find { it.id == id }
        if(actionbar == null) throw IllegalArgumentException("Unknown Actionbar ID: $id")

        if(activeActionBars.contains(id)) return

        activeActionBars.add(id)
    }

    fun disableActionBar(id: String) {
        val actionbar = actionbars.find { it.id == id }
        if(actionbar == null) throw IllegalArgumentException("Unknown Actionbar ID: $id")

        if(!activeActionBars.contains(id)) return

        activeActionBars.remove(id)
    }
}