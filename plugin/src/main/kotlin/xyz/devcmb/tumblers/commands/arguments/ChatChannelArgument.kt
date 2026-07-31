package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.player.ChatController
import xyz.devcmb.tumblers.util.Format

class ChatChannelArgument : ArgumentResolver<CommandSender, ChatController.ChatChannel>() {
    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<ChatController.ChatChannel>,
        argument: String
    ): ParseResult<ChatController.ChatChannel> {
        val channels = ChatController.ChatChannel.entries
        val channel = channels
            .filter { if(invocation.sender() is Player) it.canSend(invocation.sender() as Player) else true }
            .find { it.name.equals(argument, ignoreCase = true) }

        if(channel == null) {
            return ParseResult.failure(Format.error("Invalid channel name!"))
        }

        return ParseResult.success(channel)
    }

    override fun suggest(
        invocation: Invocation<CommandSender>,
        argument: Argument<ChatController.ChatChannel>,
        context: SuggestionContext
    ): SuggestionResult {
        return ChatController.ChatChannel.entries
            .filter { if(invocation.sender() is Player) it.canSend(invocation.sender() as Player) else true }
            .map { it.name.lowercase() }
            .stream()
            .collect(SuggestionResult.collector())
    }
}