package xyz.devcmb.tumblers.controllers

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.engine.Timer
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.formattedName
import xyz.devcmb.tumblers.util.openHandledInventory
import xyz.devcmb.tumblers.util.playerController
import java.util.UUID

@Controller("eventController", Controller.Priority.MEDIUM)
class EventController : IController {
    lateinit var teamScores: HashMap<Team, Int>

    private val databaseController: DatabaseController by lazy {
        ControllerDelegate.getController<DatabaseController>()
    }
    lateinit var topbarRunnable: BukkitRunnable
    var state: State = State.EVENT_INACTIVE

    var eventTimer: Timer? = null
    var eventTimerTitle: String? = null

    val readyCheckWaiting: ArrayList<Player> = ArrayList()
    var readyCheckAborted: Boolean = false
    var readyCheckTimer: Timer? = null

    companion object {
        @field:Configurable("event.event_mode")
        var eventMode: Boolean = false
    }

    override fun init() {
        TreeTumblers.pluginScope.launch {
            teamScores = databaseController.getTeamScores()
        }

        topbarRunnable = object : BukkitRunnable() {
            override fun run() = sendDefaultTopbar()
        }
        topbarRunnable.runTaskTimer(TreeTumblers.plugin, 0, 20)
    }

    fun startEvent() {
        TreeTumblers.pluginScope.launch {
            val ready = readyCheck()
            if(!ready) {
                suspendSync {
                    Bukkit.getOnlinePlayers().forEach { it.closeInventory() }
                }
                Bukkit.broadcast(Format.warning("Not all players ready! Ready check failed!"))
                return@launch
            }

            Bukkit.broadcast(Format.success("All players ready! Ready check success!"))
        }
    }

    suspend fun readyCheck(): Boolean {
        Bukkit.broadcast(Format.mm("<green><b>Are you ready?</b><green>"))
        Team.entries
            .filter { it.playingTeam }
            .forEach {
                readyCheckWaiting.addAll(it.getOnlinePlayers())
            }

        suspendSync {
            readyCheckWaiting.forEach { it.openHandledInventory("readyCheckInventory") }
        }

        readyCheckTimer = Timer("ready_check_expiry_${UUID.randomUUID().toString().take(6)}", 10) { early ->
            if(early) return@Timer

            readyCheckWaiting.forEach {
                Bukkit.broadcast(Format.error(
                    Format.mm(
                        "<player> is not ready!",
                        Placeholder.component("player", it.formattedName)
                    )
                ))
            }
            readyCheckWaiting.clear()
            readyCheckAborted = true
        }
        readyCheckTimer!!.start()

        while(readyCheckWaiting.isNotEmpty() && !readyCheckAborted) {
            delay(50)
        }

        val aborted = readyCheckAborted
        readyCheckAborted = false

        readyCheckTimer?.end()
        readyCheckTimer = null

        return !aborted
    }

    fun markReady(player: Player) {
        Bukkit.broadcast(Format.success(
            Format.mm(
                "<player> is ready!",
                Placeholder.component("player", player.formattedName)
            )
        ))
        readyCheckWaiting.remove(player)
    }

    fun markNotReady(player: Player) {
        Bukkit.broadcast(Format.error(
            Format.mm(
                "<player> is not ready!",
                Placeholder.component("player", player.formattedName)
            )
        ))
        readyCheckAborted = true
        readyCheckWaiting.clear()
    }
    
    fun sendDefaultTopbar() {
        var teamComponent = Component.empty()

        getEventTeamPlacements().forEachIndexed { i, it ->
            var playersComponent = Component.text(" ")
            databaseController.whitelistedPlayersCache.toSortedMap().filter { entry -> entry.value == it.first }.toList().forEachIndexed { index, entry ->
                val uuid = databaseController.whitelistedPlayerUUIDs[entry.first]!!
                val onlinePlayer = Bukkit.getPlayer(uuid)
                var name = Component.empty()
                if(index != 0) {
                    name = name.append(Component.text(" • ", NamedTextColor.WHITE))
                }

                name = name.append(Format.mm("<color:${if(onlinePlayer != null) "white" else "dark_gray"}><head:$uuid></color> ")).append(
                    if(onlinePlayer != null) Component.text(onlinePlayer.name, it.first.color)
                    else Component.text(entry.first, NamedTextColor.GRAY)
                )

                playersComponent = playersComponent.append(name)
            }
            playersComponent = playersComponent.append(Component.text(" "))

            teamComponent = teamComponent.append(
                Format.mm(
                    " <br><white>#${it.second}</white> <team><shift>${" ".repeat(60)}<gold>${teamScores[it.first]!!}</gold> <br><players><br>",
                    Placeholder.component("team", it.first.formattedName),
                    // By negative spacing, I don't need to do any math for the repetition (and don't need to use periods)
                    Placeholder.component("shift", UserInterfaceUtility.negativeSpace(UserInterfaceUtility.getPixelWidth(it.first.teamName) + 11)),
                    Placeholder.component("players", playersComponent)
                )
            )
        }

        Bukkit.getOnlinePlayers().forEach {
            it.sendPlayerListHeader(
                Component.empty()
                    .appendNewline()
                    .append(Component.text("\u0001").font(UserInterfaceUtility.ICONS))
                    .appendNewline()
                    .appendNewline()
                    .appendNewline()
                    .appendNewline()
                    .appendNewline()
                    .append(
                        Component.text("EVENT MODE: ", NamedTextColor.AQUA)
                            .append(Component.text(
                                if(eventMode) "ENABLED" else "DISABLED",
                                if(eventMode) NamedTextColor.GREEN else NamedTextColor.RED
                            ))
                    )
                    .appendNewline()
                    .append(teamComponent)
            )

            it.sendPlayerListFooter(Format.mm(
                "<aqua>Ping: <white>${it.ping}ms</white></aqua><br><aqua>Online Players: <white>${Bukkit.getOnlinePlayers().size}</white></aqua><br>"
            ))
        }
    }

    fun grantScore(player: TumblingPlayer, amount: Int) {
        player.score += amount
        teamScores.put(player.team, (teamScores[player.team] ?: 0) + amount)
    }

    fun getEventTeamPlacements(): ArrayList<Pair<Team, Int>> {
        val sorted = teamScores.entries.sortedWith(
            compareByDescending<MutableMap.MutableEntry<Team, Int>> { it.value }
                .thenBy { it.key.priority }
        )
        return MiscUtils.calculatePlacements(sorted)
    }

    fun getEventPlayerPlacements(): ArrayList<Pair<TumblingPlayer, Int>> {
        val playerScores: HashMap<TumblingPlayer, Int> = HashMap()
        playerController.players.forEach {
            playerScores.put(it, it.score)
        }

        val sorted = playerScores.entries.sortedWith(
            compareByDescending<MutableMap.MutableEntry<TumblingPlayer, Int>> { it.value }
                .thenBy { it.key.bukkitPlayer?.name }
        )
        return MiscUtils.calculatePlacements(sorted)
    }

    fun replicateScores() {
        if(!eventMode) return

        TreeTumblers.pluginScope.launch {
            DebugUtil.info("Replicating event data...")
            databaseController.replicateTeamData(teamScores)
            playerController.players.forEach {
                databaseController.replicatePlayerData(it)
            }
            DebugUtil.success("Data replication successful!")
        }
    }

    override fun cleanup() {
        replicateScores()
    }

    enum class State {
        EVENT_INACTIVE,
        PRE_EVENT,
        INTERMISSION,
        VOTING,
        NORMAL_GAME,
        FINAL_GAME,
        POST_EVENT
    }
}