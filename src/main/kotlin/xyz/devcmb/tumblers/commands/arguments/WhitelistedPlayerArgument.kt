package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.DatabaseController

class WhitelistedPlayerArgument: ArgumentResolver<CommandSender, DatabaseController.WhitelistedPlayer>() {
    val databaseController: DatabaseController by lazy {
        ControllerDelegate.getController("databaseController") as DatabaseController
    }

    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<DatabaseController.WhitelistedPlayer>,
        argument: String
    ): ParseResult<DatabaseController.WhitelistedPlayer> {
        val whitelistedPlayerNames = databaseController.whitelistedPlayersCache

        if(!whitelistedPlayerNames.containsKey(argument)) {
            return ParseResult.failure("Player not found or not whitelisted!")
        }

        return ParseResult.success(DatabaseController.WhitelistedPlayer(argument))
    }

    override fun suggest(
        invocation: Invocation<CommandSender>,
        argument: Argument<DatabaseController.WhitelistedPlayer>,
        context: SuggestionContext
    ): SuggestionResult? {
        return databaseController.whitelistedPlayersCache.toList().map { it.first }.stream().collect(SuggestionResult.collector())
    }
}