package xyz.devcmb.tumblers.controllers.player

import io.papermc.paper.util.Tick
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Material
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.IController
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.engine.Flag
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

        NametagController.updateTagVisibility(player)

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
        NametagController.updateTagVisibility(player)
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

    @EventHandler(priority = EventPriority.LOW)
    fun playerSpectateDeathEvent(event: PlayerDeathEvent) {
        val currentGame = GameController.activeGame
        if(Flag.CUSTOM_DEATH_SYSTEM in (currentGame?.data?.flags ?: emptyList())) return

        event.isCancelled = true
        event.player.showTitle(Title.title(
            Format.mm("<red><b>You died!</b></red>"),
            Component.empty(),
            Title.Times.times(Tick.of(0), Tick.of(45), Tick.of(0))
        ))

        if(currentGame != null) currentGame.makeSpectator(event.player)
        else {
            makeSpectator(event.player)
        }
    }
}