package xyz.devcmb.tumblers.commands

import dev.rollczi.litecommands.handler.result.ResultHandlerChain
import dev.rollczi.litecommands.invalidusage.InvalidUsage
import dev.rollczi.litecommands.invalidusage.InvalidUsageHandler
import dev.rollczi.litecommands.invocation.Invocation
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.util.Format


class InvalidUsageHandler : InvalidUsageHandler<CommandSender> {
    override fun handle(
        invocation: Invocation<CommandSender>,
        result: InvalidUsage<CommandSender>,
        chain: ResultHandlerChain<CommandSender>
    ) {
        val sender = invocation.sender()
        val schematic = result.schematic

        var invalidUsageMessage = Format.error("Invalid usage of command!")
        for (scheme in schematic.all()) {
            invalidUsageMessage = invalidUsageMessage.append(
                Component.newline()
                    .append(Component.text("- ", NamedTextColor.GRAY)))
                    .append(Component.text(scheme, NamedTextColor.WHITE))
        }
        sender.sendMessage(invalidUsageMessage)
    }
}