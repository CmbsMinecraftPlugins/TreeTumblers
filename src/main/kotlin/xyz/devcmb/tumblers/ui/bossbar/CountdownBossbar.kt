package xyz.devcmb.tumblers.ui.bossbar

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.NamespacedKey
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.MiscUtils
import kotlin.math.roundToInt

class CountdownBossbar(
    val gameController: GameController,
    override val id: String = "countdownBossbar",
    override val padding: Int = 0
) : HandledBossbar {

    override fun getComponent(): Component {
        val currentGame = gameController.activeGame
        if(currentGame == null) return Component.text("0:00")

        val bgSize = 30
        val text = MiscUtils.formatToMSS(currentGame.countdownTime)
        val textLength: Double = (UserInterfaceUtility.getPixelWidth(text) + (text.length - 1)).toDouble()
        val bgOffset = (textLength+((bgSize - textLength)/2)).roundToInt()
        val fullOffset = ((bgSize - textLength) / 2).roundToInt()

        return Component.empty()
            .append(UserInterfaceUtility.negativeSpace(fullOffset))
            .append(Component.text("\uEF01").font(UserInterfaceUtility.HUD))
            .append(UserInterfaceUtility.negativeSpace(bgOffset))
            .append(Component.text(text).font(NamespacedKey("tumbling", "default_shift/ascent_5")))
            .shadowColor(ShadowColor.shadowColor(0))
    }
}