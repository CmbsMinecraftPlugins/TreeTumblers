package xyz.devcmb.tumblers.ui.scoreboard.games

import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard

class PartyScoreboard(
    override val id: String = "partyScoreboard"
) : HandledScoreboard {
    override fun getObjectives(scoreboard: Scoreboard): Set<Objective> {
        return emptySet()
    }
}