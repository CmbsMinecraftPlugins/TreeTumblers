package xyz.devcmb.tumblers.controllers.games.sniffer_caretaker

import xyz.devcmb.tumblers.controllers.event.BadgeController

enum class SnifferCaretakerBadge(
    override val badgeName: String,
    override val hint: String,
    override val game: String = "sniffer_caretaker",
) : BadgeController.Badge {
    // TODO: Remove
    TEST("Test", "The quick brown fox jumps over the lazy dog")
}