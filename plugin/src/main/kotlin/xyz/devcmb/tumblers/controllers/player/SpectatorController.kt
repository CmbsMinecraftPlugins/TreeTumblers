package xyz.devcmb.tumblers.controllers.player

import org.bukkit.Material
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.IController
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.hideToAll
import xyz.devcmb.tumblers.item.advanced.AdvancedItemStack
import xyz.devcmb.tumblers.util.disableActionBar
import xyz.devcmb.tumblers.util.enableActionBar
import xyz.devcmb.tumblers.util.openHandledInventory
import xyz.devcmb.tumblers.util.showToAll

@Controller(Controller.Priority.MEDIUM)
object SpectatorController : IController {
    val spectators: ArrayList<Player> = ArrayList()

    override fun init() {
    }

    fun makeSpectator(player: Player) {
        spectators.add(player)

        player.hideToAll()
        player.heal(20.0)
        player.inventory.clear()
        player.allowFlight = true
        player.isFlying = true
        player.world.entities
            .filterIsInstance<Mob>()
            .forEach { mob ->
                if (mob.target == player) {
                    mob.target = null
                }
            }
        player.enableActionBar("spectatorActionBar")

        PlayerController.updateNametagVisibility(player)

        player.inventory.addItem(AdvancedItemStack(Material.COMPASS) {
            name(Format.mm("<green>Spectate menu</green>"))
            droppable(false)
            click {
                val inventory = GameController.activeGame?.data?.spectateInventory ?: "spectateInventory"
                player.openHandledInventory(inventory)
            }
        }.build())
    }

    fun unSpectate(player: Player) {
        if(!spectators.contains(player)) return
        spectators.remove(player)

        player.showToAll()
        player.closeInventory()
        player.inventory.remove(Material.COMPASS)
        player.isFlying = false
        player.allowFlight = false
        player.disableActionBar("spectatorActionBar")
        PlayerController.updateNametagVisibility(player)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun spectatorDamageEvent(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if(player in spectators) event.isCancelled = true
    }

    @EventHandler
    fun spectatorTargetEvent(event: EntityTargetLivingEntityEvent) {
        val player = event.target as? Player ?: return
        if(player in spectators) event.isCancelled = true
    }

    @EventHandler
    fun spectatorInteractEvent(event: PlayerInteractEvent) {
        if(event.player in spectators) event.isCancelled = true
    }

    @EventHandler
    fun spectatorAttackEvent(event: EntityDamageEvent) {
        val player = event.damageSource.causingEntity as? Player ?: return
        if(player in spectators) event.isCancelled = true
    }

    @EventHandler
    fun playerLeaveEvent(event: PlayerQuitEvent) {
        unSpectate(event.player)
    }
}