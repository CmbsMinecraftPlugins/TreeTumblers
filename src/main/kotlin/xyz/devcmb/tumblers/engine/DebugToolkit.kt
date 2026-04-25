package xyz.devcmb.tumblers.engine

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

abstract class DebugToolkit {
    abstract val events: HashMap<String, (sender: CommandSender) -> Unit>
    abstract fun killEvent(killer: Player?, killed: Player?)
    abstract fun deathEvent(killed: Player?)

    data class DebuggingEvent(val name: String)
}