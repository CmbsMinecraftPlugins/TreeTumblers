package xyz.devcmb.tumblers.commands.dev

import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format

@Suppress("unused")
@Permission("tumbling.dev")
class DebugCommand {
    @Command("debug logging subscribe <level>")
    fun executeDebug(source: CommandSourceStack, @Argument("level") loggingLevel: DebugUtil.DebugLogLevel) {
        val sender = source.sender
        if(sender !is Player) {
            sender.sendMessage(Format.error("Only players can use this command!"))
            return
        }

        DebugUtil.subscribe(sender, loggingLevel)
        sender.sendMessage(Format.success("Subscribed to the ${loggingLevel.name.lowercase()} logging channel successfully!"))
        DebugUtil.info("${sender.name} subscribed to ${loggingLevel.name} logging channel")
    }

    @Command("debug logging send <level> <message>")
    fun testLogging(@Argument("level") loggingLevel: DebugUtil.DebugLogLevel, message: Array<String>) {
        val message = message.joinToString(" ")
        when(loggingLevel) {
            DebugUtil.DebugLogLevel.INFO -> DebugUtil.info(message)
            DebugUtil.DebugLogLevel.ERROR -> DebugUtil.severe(message)
            DebugUtil.DebugLogLevel.SUCCESS -> DebugUtil.success(message)
            DebugUtil.DebugLogLevel.WARNING -> DebugUtil.warning(message)
            else -> return
        }
    }
}
