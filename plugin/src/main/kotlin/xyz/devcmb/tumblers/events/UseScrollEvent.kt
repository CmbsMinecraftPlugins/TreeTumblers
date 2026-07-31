package xyz.devcmb.tumblers.events

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack

class UseScrollEvent(val itemStack: ItemStack) : Event(), Cancellable {
    companion object {
        @JvmStatic
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlerList
        }
    }

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    private var isEventCancelled: Boolean = false
    override fun isCancelled(): Boolean {
        return isEventCancelled
    }

    override fun setCancelled(cancel: Boolean) {
        isEventCancelled = cancel
    }
}