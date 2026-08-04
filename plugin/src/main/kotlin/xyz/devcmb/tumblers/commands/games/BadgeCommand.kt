package xyz.devcmb.tumblers.commands.games

import io.papermc.paper.command.brigadier.CommandSourceStack
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import xyz.devcmb.tumblers.controllers.event.BadgeController
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.Format
import kotlin.collections.orEmpty

@Suppress("unused")
@Permission("tumbling.dev")
class BadgeCommand {
    @Command("badge grant <player> <badgeId>")
    fun executeBadge(source: CommandSourceStack, player: TumblingPlayer, @Argument(suggestions = "badge_ids") badgeId: String) {
        val sender = source.sender
        val allBadges = getBadges()
        val badge = allBadges.find { it.name.equals(badgeId, true) }

        if(badge == null) {
            sender.sendMessage(Format.error("A badge with the provided ID does not exist!"))
            return
        }

        BadgeController.grantBadge(player, badge)
        sender.sendMessage(Format.success(Format.mm("Granted the ${badge.name.lowercase()} badge to <player:${player.uuid}> successfully!")))
    }

    private fun getBadges(): List<BadgeController.Badge> {
        return GameController.games.flatMap { it.data.badges.orEmpty() }
    }

    @Suggestions("badge_ids")
    fun suggestBadgeIds(): List<String> {
        return getBadges().map { it.name }
    }
}
