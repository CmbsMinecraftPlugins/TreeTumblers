package xyz.devcmb.tumblers.ui.scoreboard

import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.RenderType

class HealthIndicatorScoreboard(
    val player: Player,
    override val displayName: String = "<red>♥</red>",
    override val id: String = "healthIndicatorScoreboard"
) : HandledScoreboard.StaticScoreboard(Criteria.HEALTH, RenderType.HEARTS)