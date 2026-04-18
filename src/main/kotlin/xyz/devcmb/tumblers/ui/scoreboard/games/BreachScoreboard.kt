package xyz.devcmb.tumblers.ui.scoreboard.games



import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard

class BreachScoreboard(override val id: String = "breachScoreboard") : HandledScoreboard {
    override fun getObjectives(scoreboard: Scoreboard): Set<Objective> {
        // TODO: BLA BLA BLA MAKE THE THING
        return emptySet()
    }
}