package xyz.devcmb.tumblers.ui.scoreboard

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.RenderType
import xyz.devcmb.tumblers.util.Format

class HealthIndicatorScoreboard(
    val player: Player,
    override val displayName: Component = Format.mm("<red>♥</red>"),
    override val id: String = "healthIndicatorScoreboard"
) : HandledScoreboard.StaticScoreboard(Criteria.HEALTH, RenderType.HEARTS)