package xyz.devcmb.tumblers.controllers

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.hideToAll
import xyz.devcmb.tumblers.util.item.AdvancedItemStack
import xyz.devcmb.tumblers.util.openHandledInventory
import xyz.devcmb.tumblers.util.showToAll

@Controller("spectatorController", Controller.Priority.MEDIUM)
class SpectatorController : IController {
    val spectators: HashMap<Player, Boolean> = HashMap()
    private var spectatorTask: BukkitRunnable? = null

    override fun init() {
        spectatorTask = object : BukkitRunnable() {
            override fun run() {
                spectators.forEach { player, bar ->
                    if(bar) player.sendActionBar(Format.mm("<gray>Spectating</gray>"))
                }
            }
        }
        spectatorTask!!.runTaskTimer(TreeTumblers.plugin, 0, 10)
    }

    override fun cleanup() {
        spectatorTask?.cancel()
    }

    fun makeSpectator(player: Player, sendActionBar: Boolean = true) {
        spectators.put(player, sendActionBar)

        player.hideToAll()
        player.heal(20.0)
        player.inventory.clear()
        player.allowFlight = true
        player.isFlying = true

        player.inventory.addItem(AdvancedItemStack(Material.COMPASS) {
            name(Format.mm("<green>Spectate menu</green>"))
            droppable(false)
            rightClick {
                player.openHandledInventory("spectateInventory")
            }
        }.build())
    }

    fun unSpectate(player: Player) {
        if(!spectators.containsKey(player)) return
        spectators.remove(player)

        player.showToAll()
        player.closeInventory()
        player.inventory.remove(Material.COMPASS)
        player.isFlying = false
        player.allowFlight = false
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun damageEvent(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if(player in spectators) event.isCancelled = true
    }

    @EventHandler
    fun playerLeaveEvent(event: PlayerQuitEvent) {
        unSpectate(event.player)
    }
}