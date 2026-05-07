package xyz.devcmb.tumblers.controllers

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.annotations.Controller

@Controller("hubController")
class HubController : IController {
    val gameController by lazy {
        ControllerDelegate.getController<GameController>()
    }

    val isHub: Boolean
        get() {
            return gameController.activeGame == null
        }

    override fun init() {
    }

    @EventHandler
    fun playerInteractEvent(event: PlayerInteractEvent) {
        if(!isHub) return
        event.isCancelled = true
    }

    @EventHandler
    fun playerDamageEvent(event: EntityDamageEvent) {
        if(!isHub || event.entity !is Player) return
        event.isCancelled = true
    }
}