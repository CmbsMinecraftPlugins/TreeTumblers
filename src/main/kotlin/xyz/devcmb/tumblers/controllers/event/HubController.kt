package xyz.devcmb.tumblers.controllers.event

import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import xyz.devcmb.tumblers.TumblingException
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.IController
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.server.WorldController
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.fadeTp
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.item.AdvancedItemStack
import xyz.devcmb.tumblers.util.openHandledInventory
import xyz.devcmb.tumblers.util.tp
import xyz.devcmb.tumblers.util.validateLocation

@Controller(Controller.Priority.MEDIUM)
object HubController : IController {
    val isHub: Boolean
        get() {
            return GameController.activeGame == null
        }

    val isVoting: Boolean
        get() {
            return EventController.state == EventController.State.VOTING
        }

    val lobbySpawnStart: List<Int> = configurable("lobby.spawn.start")
    val lobbySpawnEnd: List<Int> = configurable("lobby.spawn.end")
    val lobbySpawnYaw: Double = configurable("lobby.spawn.yaw")
    val lobbySpawnPitch: Double = configurable("lobby.spawn.pitch")
    val lobbySpawnFloor: Material = configurable("lobby.spawn.floor")
    val voidHeight: Int = configurable("lobby.void_height")

    val compass = AdvancedItemStack(Material.COMPASS) {
        name(Format.mm("<aqua>Navigator</aqua>"))
        lore(listOf(
            Format.mm("<white>Teleport to different landmarks</white>"),
            Format.mm("<white>around the <yellow>hub world!</yellow>")
        ).map { it.decoration(TextDecoration.ITALIC, false) })
        droppable(false)
        movable(false)
        rightClick {
            it.openHandledInventory("hubNavigationInventory")
        }
    }

    override fun init() {
    }

    fun spawnHub(player: Player, teleport: Boolean = true) {
        if(teleport) player.tp(getLobbyPosition())
        player.inventory.addItem(compass.build())
        BadgeController.giveCollection(player)
    }

    fun getLobbyPosition(): Location {
        val hub = Bukkit.getWorld(WorldController.lobbyWorld)!!
        val startLocation = lobbySpawnStart.validateLocation(hub)
            ?: throw TumblingException("Start location for hub spawning is not a valid location list")

        val endLocation = lobbySpawnEnd.validateLocation(hub)
            ?: throw TumblingException("End location for hub spawning is not a valid location list")

        val validSpawns: ArrayList<Location> = ArrayList()
        startLocation.forEachRegion(endLocation) {
            if(it.type == lobbySpawnFloor && it.getRelative(BlockFace.UP).isEmpty) {
                validSpawns.add(it.location.clone().add(0.0,1.0,0.0))
            }
        }

        val location = validSpawns.random().clone().toCenterLocation()
        location.pitch = lobbySpawnPitch.toFloat()
        location.yaw = lobbySpawnYaw.toFloat()
        return location
    }

    @EventHandler
    fun playerInteractEvent(event: PlayerInteractEvent) {
        if(!isHub || event.player.gameMode == GameMode.CREATIVE) return

        event.isCancelled = true
    }

    @EventHandler
    fun playerDamageEvent(event: EntityDamageEvent) {
        if(!isHub || isVoting || event.entity !is Player) return
        event.isCancelled = true
    }

    @EventHandler
    fun blockBreakEvent(event: BlockBreakEvent) {
        if(isHub && event.player.gameMode != GameMode.CREATIVE) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun playerMoveEvent(event: PlayerMoveEvent) {
        if(event.to.y > voidHeight || event.to.world.name != WorldController.lobbyWorld) return
        event.player.fadeTp(getLobbyPosition())
    }
}