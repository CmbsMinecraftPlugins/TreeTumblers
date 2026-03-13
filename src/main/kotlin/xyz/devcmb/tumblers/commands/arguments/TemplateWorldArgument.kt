package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.controllers.WorldController
import java.io.File

class TemplateWorldArgument: ArgumentResolver<CommandSender, WorldController.LoadableTemplate>() {
    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<WorldController.LoadableTemplate>,
        argument: String
    ): ParseResult<WorldController.LoadableTemplate> {
        val validPaths = getPaths()
        if(!validPaths.contains(argument)) {
            return ParseResult.failure("That is not a valid path")
        }

        val parentDir = File(WorldController.worldRoot)
        return ParseResult.success(WorldController.LoadableTemplate(
            File(parentDir, argument)
        ))
    }

    override fun suggest(
        invocation: Invocation<CommandSender>,
        argument: Argument<WorldController.LoadableTemplate>,
        context: SuggestionContext
    ): SuggestionResult {
        return getPaths().stream().collect(SuggestionResult.collector())
    }

    private fun getPaths(): ArrayList<String> {
        val suggestions: ArrayList<String> = ArrayList()

        fun scanWorldsFolder(parent: File, pathString: String) {
            parent.listFiles().forEach {
                if(!it.isDirectory) return@forEach

                if(File(it, "paper-world.yml").exists()) {
                    suggestions.add("$pathString${it.name}")
                } else {
                    scanWorldsFolder(it, "$pathString${it.name}/")
                }
            }
        }

        scanWorldsFolder(
            File(WorldController.worldRoot),
            ""
        )

        return suggestions
    }
}