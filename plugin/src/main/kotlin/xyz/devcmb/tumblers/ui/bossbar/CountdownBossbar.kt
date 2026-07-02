package xyz.devcmb.tumblers.ui.bossbar

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.NamespacedKey
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.Font
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.formatToMSS

class CountdownBossbar : HandledBossbar {
    override val id: String = "countdownBossbar"
    override val padding: Int = 0

    override fun getComponent(): Component {
        val currentGame = GameController.activeGame ?: return Component.text("0:00")

        if(currentGame.currentTimer?.paused == true) {
            return UserInterfaceUtility.backgroundTextCenter(
                Font.getGlyph("hud/countdown_paused_bg").shadowColor(ShadowColor.shadowColor(0)),
                Format.mm("<yellow>Paused</yellow>").font(NamespacedKey(TreeTumblers.NAMESPACE, "default_shift/ascent_5")),
                "Paused",
                75.0
            )
        }

        val text = formatToMSS(currentGame.countdownTime)
        return UserInterfaceUtility.backgroundTextCenter(
            Font.getGlyph("hud/countdown_bg").shadowColor(ShadowColor.shadowColor(0)),
            Component.text(text).font(NamespacedKey(TreeTumblers.NAMESPACE, "default_shift/ascent_5")),
            text,
            30.0
        )
    }
}