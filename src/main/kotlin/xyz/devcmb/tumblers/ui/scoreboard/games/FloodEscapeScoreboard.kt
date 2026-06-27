package xyz.devcmb.tumblers.ui.scoreboard.games

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard

class FloodEscapeScoreboard : HandledScoreboard.SidebarScoreboard() {
    override val displayName: String = "<blue>Flood Escape</blue> <dark_gray>|</dark_gray> <gray>Game <game>/<total></gray>"
    override val id: String = "floodEscapeScoreboard"

    override fun getLines(): ArrayList<Component> {
        return ArrayList((0..5).map { Component.text("fish", NamedTextColor.AQUA) })
    }
}