package xyz.devcmb.tumblers.events

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import xyz.devcmb.tumblers.util.item.AdvancedItemStackContext

class UseAdvancedItemEvent(val ctx: AdvancedItemStackContext) : Event(), Cancellable {
    private var isEventCancelled: Boolean = false

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    override fun isCancelled(): Boolean {
        return isEventCancelled
    }

    override fun setCancelled(cancel: Boolean) {
        isEventCancelled = cancel
    }

    companion object {
        @JvmStatic
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlerList
        }
    }
}