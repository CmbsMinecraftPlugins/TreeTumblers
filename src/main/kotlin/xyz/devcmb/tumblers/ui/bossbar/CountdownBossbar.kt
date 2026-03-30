package xyz.devcmb.tumblers.ui.bossbar

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.NamespacedKey
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.MiscUtils
class CountdownBossbar(
    val gameController: GameController,
    override val id: String = "countdownBossbar",
    override val padding: Int = 0
) : HandledBossbar {

    override fun getComponent(): Component {
        val currentGame = gameController.activeGame
        if(currentGame == null) return Component.text("0:00")

        val text = MiscUtils.formatToMSS(currentGame.countdownTime)
        return UserInterfaceUtility.backgroundTextCenter(
            Component.text("\uEF01").font(UserInterfaceUtility.HUD).shadowColor(ShadowColor.shadowColor(0)),
            Component.text(text).font(NamespacedKey("tumbling", "default_shift/ascent_5")),
            text,
            30.0
        )
    }
}