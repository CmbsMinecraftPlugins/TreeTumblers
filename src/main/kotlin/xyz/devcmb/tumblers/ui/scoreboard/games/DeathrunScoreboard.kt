package xyz.devcmb.tumblers.ui.scoreboard.games

import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard

class DeathrunScoreboard(override val id: String = "deathrunScoreboard") : HandledScoreboard {
    override fun getObjectives(scoreboard: Scoreboard): Set<Objective> {
        // TODO
        return emptySet()
    }
}