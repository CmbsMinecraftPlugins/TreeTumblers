package xyz.devcmb.tumblers.engine

import org.bukkit.command.CommandSender

abstract class DebugToolkit {
    abstract val events: HashMap<String, (sender: CommandSender) -> Unit>
    data class DebuggingEvent(val name: String)
}