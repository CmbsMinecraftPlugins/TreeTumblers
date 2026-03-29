package xyz.devcmb.tumblers.ui.bossbar

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.NamespacedKey
import xyz.devcmb.tumblers.Constants
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import kotlin.math.roundToInt

class DebugBossbar(override val id: String = "debugBossbar", override val padding: Int = 0) : HandledBossbar {
    override fun getComponent(): Component {
        val text = "TreeTumblers | ${Constants.BRANCH} (${Constants.VERSION})"

        val bgSize = 200.0
        val textLength: Double = UserInterfaceUtility.getPixelWidth(text).toDouble()
        val bgOffset = (textLength+((bgSize - textLength)/2.0)).roundToInt()
        val fullOffset = ((bgSize - textLength) / 2.0).roundToInt()

        return Component.empty()
            .append(UserInterfaceUtility.negativeSpace(fullOffset))
            .append(Component.text("\uEF02").font(UserInterfaceUtility.HUD).shadowColor(ShadowColor.shadowColor(0)))
            .append(UserInterfaceUtility.negativeSpace(bgOffset))
            .append(Component.empty()
                .append(Component.text("TreeTumblers", NamedTextColor.GREEN))
                .append(Component.text(" ").font(NamespacedKey("minecraft", "default")))
                .append(Component.text("|", NamedTextColor.WHITE))
                .append(Component.text(" ").font(NamespacedKey("minecraft", "default")))
                .append(Component.text(Constants.BRANCH, NamedTextColor.GOLD))
                .append(Component.text(" ").font(NamespacedKey("minecraft", "default")))
                .append(Component.text("(${Constants.VERSION})", NamedTextColor.GRAY))
                .font(NamespacedKey("tumbling", "default_shift/ascent_5"))
            )
    }
}