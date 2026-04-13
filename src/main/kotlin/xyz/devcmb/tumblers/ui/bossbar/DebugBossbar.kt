package xyz.devcmb.tumblers.ui.bossbar

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.NamespacedKey
import xyz.devcmb.tumblers.Constants
import xyz.devcmb.tumblers.ui.UserInterfaceUtility

class DebugBossbar(override val id: String = "debugBossbar", override val padding: Int = 0) : HandledBossbar {
    override fun getComponent(): Component {
        val text = "TreeTumblers | ${Constants.BRANCH} (${Constants.VERSION})"

        return UserInterfaceUtility.backgroundTextCenter(
            Component.text("\uEF02").font(UserInterfaceUtility.HUD).shadowColor(ShadowColor.shadowColor(0)),
            Component.empty()
                .append(Component.text("TreeTumblers", NamedTextColor.GREEN))
                .append(Component.text(" ").font(NamespacedKey("minecraft", "default")))
                .append(Component.text("|", NamedTextColor.WHITE))
                .append(Component.text(" ").font(NamespacedKey("minecraft", "default")))
                .append(Component.text(Constants.BRANCH, NamedTextColor.GOLD))
                .append(Component.text(" ").font(NamespacedKey("minecraft", "default")))
                .append(Component.text("(${Constants.VERSION})", NamedTextColor.GRAY))
                .font(NamespacedKey("tumbling", "default_shift/ascent_5")),
            text,
            250.0
        )
    }
}