package xyz.devcmb.tumblers.commands.dev

import com.github.retrooper.packetevents.protocol.item.ItemStack
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import dev.rollczi.litecommands.programmatic.LiteCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.item.ItemRegistry
import xyz.devcmb.tumblers.util.Format

@Command(name = "item")
@Permission("tumbling.dev")
class ItemCommand {
    @Execute(name = "give")
    fun execute(@Context sender: Player, @Arg item: ItemRegistry.CustomItemDefinition) {
        ItemRegistry.give(sender, item.id)
        sender.sendMessage(Format.success("Gave item successfully!"))
    }
}