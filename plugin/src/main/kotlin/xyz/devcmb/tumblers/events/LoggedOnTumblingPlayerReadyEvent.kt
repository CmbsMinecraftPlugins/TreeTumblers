package xyz.devcmb.tumblers.events

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import xyz.devcmb.tumblers.data.TumblingPlayer

class LoggedOnTumblingPlayerReadyEvent(val bukkitPlayer: Player, val tumblingPlayer: TumblingPlayer) : Event() {
    override fun getHandlers(): HandlerList {
        return handlerList
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