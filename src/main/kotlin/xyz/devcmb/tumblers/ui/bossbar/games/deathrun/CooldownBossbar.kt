package xyz.devcmb.tumblers.ui.bossbar.games.deathrun

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.games.deathrun.DeathrunController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.bossbar.HandledBossbar
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format

class CooldownBossbar(
    val player: Player,
    val gameController: GameController,
    override val id: String = "deathrunCooldownBossbar",
    override val padding: Int = 0
) : HandledBossbar {
    override fun getComponent(): Component {
        val activeGame = gameController.activeGame
        if(activeGame == null || activeGame !is DeathrunController) return Component.text(DebugUtil.DebugLogLevel.ERROR.icon).font(UserInterfaceUtility.WARNINGS)

        val currentTrap = activeGame.currentTraps[player]
        if(currentTrap == null) return Component.text(DebugUtil.DebugLogLevel.ERROR.icon).font(UserInterfaceUtility.WARNINGS)

        val cooldownTime = activeGame.cooldownTimes.get(currentTrap)
        return if(cooldownTime != null) {
            val trap = activeGame.mapTraps[activeGame.roundIndex]!![currentTrap]
            val elapsedSeconds = (System.currentTimeMillis() - cooldownTime) / 1000L
            val timeLeft = (trap.cooldown.toLong() - elapsedSeconds).coerceAtLeast(0L)
            UserInterfaceUtility.backgroundTextCenter(
                Component.text("\uEF02")
                    .shadowColor(ShadowColor.shadowColor(0))
                    .font(DeathrunController.font),
                Format.mm("<red>On cooldown! <white>${timeLeft}s</white></red>"),
                "On cooldown! ${timeLeft}s",
                175.0
            )
        } else {
            UserInterfaceUtility.backgroundTextCenter(
                Component.text("\uEF01")
                    .shadowColor(ShadowColor.shadowColor(0))
                    .font(DeathrunController.font),
                Format.mm("<green>Off cooldown!</green>"),
                "Off cooldown!",
                175.0
            )
        }
    }
}