package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.item.custom.ItemRegistry
import xyz.devcmb.tumblers.util.Format

class CustomItemArgument : ArgumentResolver<CommandSender, ItemRegistry.CustomItemDefinition>() {
    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<ItemRegistry.CustomItemDefinition>,
        argument: String
    ): ParseResult<ItemRegistry.CustomItemDefinition> {
        return if(ItemRegistry.items[argument] != null) ParseResult.success(ItemRegistry.CustomItemDefinition(argument))
            else ParseResult.failure(Format.error("Item not found!"))
    }

    override fun suggest(
        invocation: Invocation<CommandSender>,
        argument: Argument<ItemRegistry.CustomItemDefinition>,
        context: SuggestionContext
    ): SuggestionResult {
        return ItemRegistry.items.keys.stream().collect(SuggestionResult.collector())
    }
}