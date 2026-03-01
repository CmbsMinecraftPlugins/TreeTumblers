package xyz.devcmb.playground.commands.dev

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.util.DebugUtil
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
                Component.text(
                    "You are currently in the ${DebugUtil.loggingSubscriptions.getOrElse(player, { DebugUtil.DebugLogLevel.NONE })} logging group",
                    NamedTextColor.YELLOW
                )
            )
            return
        }

        DebugUtil.subscribe(player, loggingLevel)
        player.sendMessage(Component.text("Subscribed to the ${loggingLevel.name.lowercase()} logging channel successfully!", NamedTextColor.GREEN))

        DebugUtil.log("${player.name} subscribed to ${loggingLevel.name} logging channel", DebugUtil.DebugLogLevel.INFO)
    }
}
