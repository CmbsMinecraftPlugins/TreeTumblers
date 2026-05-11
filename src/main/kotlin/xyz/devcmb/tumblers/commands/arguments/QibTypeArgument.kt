package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.controllers.player.NoxesiumController
import xyz.devcmb.tumblers.util.Format

class QibTypeArgument: ArgumentResolver<CommandSender, NoxesiumController.QibType>() {
    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<NoxesiumController.QibType>,
        argument: String
    ): ParseResult<NoxesiumController.QibType> {
        val types = NoxesiumController.QibType.entries
        val enum = types.find { it.name.lowercase() == argument.lowercase() }

        if(enum == null) {
            return ParseResult.failure(Format.error("That isn't a valid Qib Type"))
        }

        return ParseResult.success(enum)
    }

    override fun suggest(
        invocation: Invocation<CommandSender>,
        argument: Argument<NoxesiumController.QibType>,
        context: SuggestionContext
    ): SuggestionResult? {
        return NoxesiumController.QibType.entries
            .stream()
            .map { it.name.lowercase() }
            .collect(SuggestionResult.collector())
    }
}