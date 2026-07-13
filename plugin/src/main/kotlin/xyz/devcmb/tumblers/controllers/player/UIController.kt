package xyz.devcmb.tumblers.controllers.player

import org.bukkit.NamespacedKey
import xyz.devcmb.fui.FontUI
import xyz.devcmb.fui.builder.buildFontUI
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.IController
import java.io.File

@Controller
object UIController : IController {
    lateinit var fUI: FontUI
    override fun init() {
        fUI = buildFontUI(File(TreeTumblers.plugin.dataFolder, "pack.zip")) {
            registerFont(NamespacedKey(TreeTumblers.NAMESPACE, "icons"))
            registerFont(NamespacedKey(TreeTumblers.NAMESPACE, "hud"))
            registerFont(NamespacedKey(TreeTumblers.NAMESPACE, "containers"))
            registerFont(NamespacedKey(TreeTumblers.NAMESPACE, "games"))

            registerDefaultAscents(-4, 4, -8, 8, -12, 12, -16, 16, -20, 20, -24, 24, -28, 28, -32, 32, -36, 36, -40, 40, 44, -44, 48, -48) {
                NamespacedKey(TreeTumblers.NAMESPACE, "offset/default_offset_$it")
            }

            registerSpacingCharacters(buildMap {
                put('\uE000', 0.5)
                for (i in 1..11) {
                    put('\uE000' + i, (1 shl (i - 1)).toDouble())
                }

                put('\uF000', -0.5)
                for (i in 1..11) {
                    put('\uF000' + i, -(1 shl (i - 1)).toDouble())
                }
            }, NamespacedKey(TreeTumblers.NAMESPACE, "spaces"))
        }
    }
}