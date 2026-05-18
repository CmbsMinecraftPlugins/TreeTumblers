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
import xyz.devcmb.tumblers.TumblingGenericException
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.ControllerBase
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.fadeTp
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.item.AdvancedItemStack
import xyz.devcmb.tumblers.util.openHandledInventory
import xyz.devcmb.tumblers.util.tp
import xyz.devcmb.tumblers.util.validateLocation

@Controller(Controller.Priority.MEDIUM)
class HubController : ControllerBase() {
    val gameController: GameController by controller()
    val eventController: EventController by controller()
    val badgeController: BadgeController by controller()

    val isHub: Boolean
        get() {
            return gameController.activeGame == null
        }

    val isVoting: Boolean
        get() {
            return eventController.state == EventController.State.VOTING
        }

    companion object {
        @field:Configurable("lobby.world")
        var lobbyWorld: String = "world"

        @field:Configurable("lobby.spawn.start")
        var lobbySpawnStart: List<Int> = listOf(-56, 190, 13)

        @field:Configurable("lobby.spawn.end")
        var lobbySpawnEnd: List<Int> = listOf(-80,190,3)

        @field:Configurable("lobby.spawn.yaw")
        var lobbySpawnYaw: Double = -90.0

        @field:Configurable("lobby.spawn.pitch")
        var lobbySpawnPitch: Double = 0.0

        @field:Configurable("lobby.spawn.floor")
        var lobbySpawnFloor: Material = Material.STONE_BRICKS

        @field:Configurable("lobby.void_height")
        var voidHeight: Int = 177
    }

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

    fun spawnHub(player: Player) {
        player.tp(getLobbyPosition())
        player.inventory.addItem(compass.build())
        badgeController.giveCollection(player)
    }

    fun getLobbyPosition(): Location {
        val hub = Bukkit.getWorld(lobbyWorld)!!
        val startLocation = lobbySpawnStart.validateLocation(hub)
            ?: throw TumblingGenericException("Start location for hub spawning is not a valid location list")

        val endLocation = lobbySpawnEnd.validateLocation(hub)
            ?: throw TumblingGenericException("End location for hub spawning is not a valid location list")

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
        if(event.to.y > voidHeight || event.to.world.name != lobbyWorld) return
        event.player.fadeTp(getLobbyPosition())
    }
}