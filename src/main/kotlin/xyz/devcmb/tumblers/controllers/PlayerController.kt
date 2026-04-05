package xyz.devcmb.tumblers.controllers

import io.papermc.paper.connection.PlayerLoginConnection
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.attribute.Attribute
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.devcmb.tumblers.Constants
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.ui.PlayerUIController
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.item.AdvancedItemRegistry
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.unpackCoordinates
import java.util.UUID

@Controller("playerController", Controller.Priority.MEDIUM)
class PlayerController : IController {
    val playerUIControllers: HashMap<Player, PlayerUIController> = HashMap()
    val hiddenPlayers: MutableSet<Player> = HashSet()
    lateinit var players: ArrayList<TumblingPlayer>

    private val databaseController: DatabaseController by lazy {
        ControllerDelegate.getController("databaseController") as DatabaseController
    }

    companion object {
        @field:Configurable("lobby.world")
        var lobbyWorld = "world"

        @field:Configurable("lobby.position")
        var lobbySpawn: List<Double> = listOf(0.0, 127.0, 0.0)
    }

    override fun init() {
        TreeTumblers.pluginScope.launch {
            players = databaseController.getAllPlayerData()
        }
    }

    fun registerTumblingPlayer(uuid: UUID, name: String, team: Team, score: Int) {
        players.add(TumblingPlayer(
            Bukkit.getPlayer(uuid),
            uuid,
            name,
            team,
            score
        ))
    }

    fun unregisterTumblingPlayer(uuid: UUID) {
        players.removeIf { it.uuid == uuid }
    }

    fun setPlayerTeam(uuid: UUID, team: Team) {
        players.find { it.uuid == uuid }!!.team = team
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun playerJoin(event: PlayerJoinEvent) {
        val player = event.player
        player.inventory.clear()
        player.gameMode = GameMode.SURVIVAL
        player.isFlying = false
        player.allowFlight = false
        player.clearActivePotionEffects()
        player.teleport(lobbySpawn.unpackCoordinates(Bukkit.getWorld(lobbyWorld)!!))
        player.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 20.0
        player.health = 20.0

        Bukkit.getOnlinePlayers().forEach {
            player.unlistPlayer(it)
            it.unlistPlayer(player)
        }

        hiddenPlayers.forEach {
            player.hidePlayer(TreeTumblers.plugin, it)
        }

        event.joinMessage(Component.empty())
        playerUIControllers.put(player, PlayerUIController(player))

        if(Constants.IS_DEVELOPMENT) {
            DebugUtil.subscribe(player, DebugUtil.DebugLogLevel.WARNING)
            player.sendMessage(
                Component.text("Developer mode is active. You have automatically been subscribed to the warning debug channel.")
                    .color(NamedTextColor.YELLOW)
            )
        }

        val tumblingPlayer = players.find { it.uuid == player.uniqueId }!!
        tumblingPlayer.bukkitPlayer = player

        player.displayName(Format.formatPlayerName(tumblingPlayer))
        Bukkit.broadcast(
            Component.text("[").color(NamedTextColor.GRAY)
                .append(Component.text("+").color(NamedTextColor.GREEN))
                .append(Component.text("] ").color(NamedTextColor.GRAY))
                .append(Format.formatPlayerName(tumblingPlayer).color(NamedTextColor.WHITE))
        )
    }

    @EventHandler
    fun playerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val tumblingPlayer = player.tumblingPlayer

        hiddenPlayers.remove(player)

        playerUIControllers[player]?.cleanup()
        playerUIControllers.remove(player)

        event.quitMessage(
            Component.text("[").color(NamedTextColor.GRAY)
                .append(Component.text("-").color(NamedTextColor.RED))
                .append(Component.text("] ").color(NamedTextColor.GRAY))
                .append(Format.formatPlayerName(tumblingPlayer).color(NamedTextColor.WHITE))
        )

        tumblingPlayer.bukkitPlayer = null
    }

    @EventHandler
    fun playerInteract(event: PlayerInteractEvent) {
        AdvancedItemRegistry.handleInteract(event)
    }

    @EventHandler
    fun playerDropItem(event: PlayerDropItemEvent) {
        AdvancedItemRegistry.handleDrop(event)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun playerFireworkDamageEvent(event: EntityDamageEvent) {
        if(event.entity is Player && event.damageSource.damageType == DamageType.FIREWORKS)
            event.isCancelled = true
    }

    /*
     * This uses an unstable api for preventing a player from connecting in the first place
     * If this ever becomes a problem, either update it to a new, supported method
     * Or just resort to a kick-on-join thing
     */
    @EventHandler
    @Suppress("UnstableApiUsage")
    fun playerLoginEvent(event: PlayerConnectionValidateLoginEvent) {
        val connection = event.connection
        if(connection !is PlayerLoginConnection) return

        val uuid = connection.authenticatedProfile?.id

        runBlocking {
            if(!databaseController.isWhitelisted(uuid.toString())) {
                event.kickMessage(
                    Component.text("———————————————————————————————", NamedTextColor.RED)
                        .append(Component.newline())
                        .append(Component.text("You are not whitelisted!", NamedTextColor.RED))
                        .append(Component.newline())
                        .append(Component.text("———————————————————————————————", NamedTextColor.RED))
                )
            }
        }
    }
}