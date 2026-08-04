package xyz.devcmb.tumblers.commands.dev

import io.papermc.paper.command.brigadier.CommandSourceStack
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import xyz.devcmb.tumblers.commands.requirePlayer
import xyz.devcmb.tumblers.item.custom.ItemRegistry
import xyz.devcmb.tumblers.util.Format

@Suppress("unused")
@Permission("tumbling.dev")
class ItemCommand {
    @Command("item give <item>")
    fun execute(
        source: CommandSourceStack,
        @Argument(suggestions = "items") item: String
    ) {
        val sender = source.sender.requirePlayer() ?: return

        if(item !in ItemRegistry.items.keys) {
            sender.sendMessage(Format.error("Item $item was not found in the item registry!"))
            return
        }

        ItemRegistry.give(sender, item)
        sender.sendMessage(Format.success("Gave item successfully!"))
    }

//    @Parser
//    fun customItemDefinitionParser(
//        context: CommandContext<CommandSourceStack>,
//        commandInput: CommandInput
//    ): ItemRegistry.CustomItemDefinition {
//        val input = commandInput.readString()
//
//        return if(ItemRegistry.items[input] != null) ItemRegistry.CustomItemDefinition(input)
//        else throw IllegalArgumentException("Unknown custom item '$input'")
//    }

    @Suggestions("items")
    fun suggestItemDefinitions(
        context: CommandContext<CommandSourceStack>,
        commandInput: CommandInput
    ): List<String> {
        return ItemRegistry.items.keys.toList()
    }
}