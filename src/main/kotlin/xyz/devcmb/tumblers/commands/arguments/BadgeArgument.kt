package xyz.devcmb.tumblers.commands.arguments

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerRegistry
import xyz.devcmb.tumblers.controllers.event.BadgeController
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.util.Format

class BadgeArgument : ArgumentResolver<CommandSender, BadgeController.Badge>() {
    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<BadgeController.Badge>,
        argument: String
    ): ParseResult<BadgeController.Badge> {
        val badges = getBadges()
        val argumentBadge = badges.find { it.name.equals(argument, true) }
        if(argumentBadge == null) {
            return ParseResult.failure(Format.error("Badge with the id $argument was not found!"))
        }

        return ParseResult.success(argumentBadge)
    }

    override fun suggest(
        invocation: Invocation<CommandSender>,
        argument: Argument<BadgeController.Badge>,
        context: SuggestionContext
    ): SuggestionResult {
        return getBadges()
            .map { it.name.lowercase() }
            .stream()
            .collect(SuggestionResult.collector())
    }

    fun getBadges(): List<BadgeController.Badge> {
        return GameController.games.flatMap { it.badges.orEmpty() }
    }
}