package xyz.devcmb.tumblers.ui.bossbar.games.deathrun

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.deathrun.DeathrunController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.bossbar.HandledBossbar
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Font
import xyz.devcmb.tumblers.util.Format

class CooldownBossbar(
    val player: Player
) : HandledBossbar {
    override val id: String = "deathrunCooldownBossbar"
    override val padding: Int = 0

    override fun getComponent(): Component {
        val activeGame = GameController.activeGame
        if(activeGame == null || activeGame !is DeathrunController) return DebugUtil.DebugLogLevel.ERROR.icon()

        val currentTrap = activeGame.currentTraps[player] ?: return DebugUtil.DebugLogLevel.ERROR.icon()

        val cooldownTime = activeGame.cooldownTimes[currentTrap]
        return if(cooldownTime != null) {
            val trap = activeGame.mapTraps[activeGame.roundIndex]!![currentTrap]
            val elapsedSeconds = (System.currentTimeMillis() - cooldownTime) / 1000L
            val timeLeft = (trap.cooldown.toLong() - elapsedSeconds).coerceAtLeast(0L)
            UserInterfaceUtility.backgroundTextCenter(
                Font.getGlyph("hud/deathrun_trap_cooldown_active")
                    .shadowColor(ShadowColor.shadowColor(0)),
                Format.mm("<red>On cooldown! <white>${timeLeft}s</white></red>"),
                "On cooldown! ${timeLeft}s",
                175.0
            )
        } else {
            UserInterfaceUtility.backgroundTextCenter(
                Font.getGlyph("hud/deathrun_trap_cooldown_inactive")
                    .shadowColor(ShadowColor.shadowColor(0)),
                Format.mm("<green>Off cooldown!</green>"),
                "Off cooldown!",
                175.0
            )
        }
    }
}