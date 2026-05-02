package xyz.devcmb.tumblers.ui.scoreboard

import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.RenderType
import org.bukkit.scoreboard.Scoreboard
import xyz.devcmb.tumblers.util.Format

class HealthIndicatorScoreboard(
    val player: Player,
    override val id: String = "healthIndicatorScoreboard"
) : HandledScoreboard {
    override fun getObjectives(scoreboard: Scoreboard): Set<Objective> {
        val objective = scoreboard.registerNewObjective(
            "healthIndicator",
            Criteria.HEALTH,
            Format.mm("<red>♥</red>"),
            RenderType.HEARTS
        )

        objective.displaySlot = DisplaySlot.BELOW_NAME

        return setOf(objective)
    }
}