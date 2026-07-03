package xyz.devcmb.tumblers.controllers.games.crumble

import xyz.devcmb.tumblers.controllers.event.BadgeController

enum class CrumbleBadge(
    override val badgeName: String,
    override val hint: String,
    override val game: String = "crumble"
) : BadgeController.Badge {
    // TODO: Test
    DOUBLE_BOOM("Double Boom", "Get a double kill with Bomber's nuke."),
    // TODO: Implement
    ACE("Ace", "Kill all 4 members of the opposing team."),
    // TODO: Implement
    SMOKY_GLORY("Smoky Glory", "Kill a player while inside a smoke bomb."),
    BATTLE_WORKER("Battle Worker", "Spleef someone using Worker's Mega Mine ability")
}