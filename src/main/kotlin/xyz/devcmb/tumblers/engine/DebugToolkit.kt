package xyz.devcmb.tumblers.engine

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

abstract class DebugToolkit {
    // TODO: Add arguments to the lambda (probably just a varargs string param in the command that i'll have to convert to a player or whatever manually)
    abstract val events: HashMap<String, (sender: CommandSender) -> Unit>
    abstract fun killEvent(killer: Player?, killed: Player?)
    abstract fun deathEvent(killed: Player?)

    data class DebuggingEvent(val name: String)
}