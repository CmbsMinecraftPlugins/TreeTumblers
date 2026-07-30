package xyz.devcmb.tumblers.controllers.server

import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockFormEvent
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.IController

@Controller
object GlobalListenerController : IController {
    /**
     * Gets called when the server has started the plugin during the onLoad cycle
     */
    override fun init() {
    }

    @EventHandler
    fun copperOxidizeEvent(event: BlockFormEvent) {
        if (event.newState.type.isBlock && event.newState.type.name.contains("COPPER")) {
            event.isCancelled = true
        }
    }
}