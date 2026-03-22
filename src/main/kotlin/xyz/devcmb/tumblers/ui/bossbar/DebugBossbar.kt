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
        val text = "Tree Tumblers | v${Constants.VERSION} (${if(Constants.IS_DEVELOPMENT) "indev" else "production"})"

        val bgSize = 200.0
        val manualOffset = 4
        val textLength: Double = (UserInterfaceUtility.getPixelWidth(text) + (text.length - 1)).toDouble()
        val bgOffset = (textLength+((bgSize - textLength)/2.0)).roundToInt() - manualOffset
        val fullOffset = ((bgSize - textLength) / 2.0).roundToInt() + manualOffset

        return Component.empty()
            .append(UserInterfaceUtility.negativeSpace(fullOffset))
            .append(Component.text("\uEF02").font(UserInterfaceUtility.HUD).shadowColor(ShadowColor.shadowColor(0)))
            .append(UserInterfaceUtility.negativeSpace(bgOffset))
            .append(Component.empty()
                .append(Component.text("Tree Tumblers", NamedTextColor.GREEN))
                .append(Component.text(" ").font(NamespacedKey("minecraft", "default")))
                .append(Component.text("|", NamedTextColor.WHITE))
                .append(Component.text(" ").font(NamespacedKey("minecraft", "default")))
                .append(Component.text("v${Constants.VERSION}", NamedTextColor.GRAY))
                .append(Component.text(" ").font(NamespacedKey("minecraft", "default")))
                .append(Component.text(if(Constants.IS_DEVELOPMENT) "(indev)" else "(production)", NamedTextColor.GOLD))
                .font(NamespacedKey("tumbling", "default_shift/ascent_5"))
            )
    }
}