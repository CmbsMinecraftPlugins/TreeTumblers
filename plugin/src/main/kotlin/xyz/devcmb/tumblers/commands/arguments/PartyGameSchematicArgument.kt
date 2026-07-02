package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.controllers.games.party.PartyController
import xyz.devcmb.tumblers.util.Format
import java.io.File

class PartyGameSchematicArgument: ArgumentResolver<CommandSender, PartyController.PartyGameSchematic>() {
    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<PartyController.PartyGameSchematic>,
        argument: String
    ): ParseResult<PartyController.PartyGameSchematic> {
        val validPaths = getPaths()
        if(!validPaths.contains(argument)) {
            return ParseResult.failure(Format.error("That is not a valid path"))
        }

        val parentDir = File(PartyController.partyGamesDirectory)
        return ParseResult.success(PartyController.PartyGameSchematic(
            File(parentDir, argument)
        ))
    }

    override fun suggest(
        invocation: Invocation<CommandSender>,
        argument: Argument<PartyController.PartyGameSchematic>,
        context: SuggestionContext
    ): SuggestionResult {
        return getPaths().stream().collect(SuggestionResult.collector())
    }

    private fun getPaths(): ArrayList<String> {
        val suggestions: ArrayList<String> = ArrayList()
        val searchDir = File(PartyController.partyGamesDirectory)

        searchDir.listFiles().forEach { parent ->
            if(parent.isDirectory) {
                parent.listFiles().forEach {
                    if(!it.isDirectory) {
                        suggestions.add("${parent.name}/${it.name}")
                    }
                }
            }
        }

        return suggestions
    }
}