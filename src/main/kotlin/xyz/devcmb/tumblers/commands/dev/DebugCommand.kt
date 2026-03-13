package xyz.devcmb.tumblers.commands.dev

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.join.Join
import dev.rollczi.litecommands.annotations.permission.Permission
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@Command(name = "debug")
@Permission("tumbling.dev")
class DebugCommand {
    @Execute(name = "logging subscribe")
    fun executeDebug(@Context player: Player, @Arg loggingLevel: Optional<DebugUtil.DebugLogLevel>) {
        val loggingLevel = loggingLevel.getOrNull()
        if(loggingLevel == null) {
            player.sendMessage(
                Format.info("You are currently in the ${DebugUtil.loggingSubscriptions.getOrElse(player, { DebugUtil.DebugLogLevel.NONE })} logging group")
            )
            return
        }

        DebugUtil.subscribe(player, loggingLevel)
        player.sendMessage(Format.success("Subscribed to the ${loggingLevel.name.lowercase()} logging channel successfully!"))
        DebugUtil.log("${player.name} subscribed to ${loggingLevel.name} logging channel", DebugUtil.DebugLogLevel.INFO)
    }

    @Execute(name = "logging send")
    fun testLogging(@Arg loggingLevel: DebugUtil.DebugLogLevel, @Join message: String) {
        DebugUtil.log(message, loggingLevel)
    }
}
