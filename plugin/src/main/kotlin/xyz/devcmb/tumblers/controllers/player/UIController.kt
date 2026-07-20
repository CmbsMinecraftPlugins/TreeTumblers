package xyz.devcmb.tumblers.controllers.player

import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.NamespacedKey
import org.bukkit.block.Barrel
import org.bukkit.block.BlastFurnace
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import org.bukkit.block.Furnace
import org.bukkit.block.Smoker
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.InventoryOpenEvent
import xyz.devcmb.fui.FontUI
import xyz.devcmb.fui.builder.buildFontUI
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.IController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.Format
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

            // TODO: Pipe in this data directly from the resource pack instead of hardcoding
            registerDefaultAscents(
                -3, 3, -4, 4, -8, 8, -12, 12, -16, 16, -20, 20, -22, 22, -24, 24, -28, 28, -32, 32, -36, 36, -40, 40, 44, -44, 48, -48
            ) {
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

    @EventHandler
    fun inventoryOpenEvent(event: InventoryOpenEvent) {
        val text = when (event.inventory.holder) {
            is Chest -> UserInterfaceUtility.centerInventoryTitle(Format.mm("<white>Small Chest</white>"))
            is DoubleChest -> UserInterfaceUtility.centerInventoryTitle(Format.mm("<white>Large Chest</white>"))

            is BlastFurnace -> Format.mm("<white><font:tumbling:offset/default_offset_-3>Blast Furnace</font></white>")
                .shadowColor(ShadowColor.shadowColor(0x3F, 0x3F, 0x3F, 0x3F))
            is Smoker -> Format.mm("<white><font:tumbling:offset/default_offset_-3>Smoker</font></white>")
                .shadowColor(ShadowColor.shadowColor(0x3F, 0x3F, 0x3F, 0x3F))
            is Furnace -> Format.mm("<white><font:tumbling:offset/default_offset_-3>Furnace</font></white>")
                .shadowColor(ShadowColor.shadowColor(0x3F, 0x3F, 0x3F, 0x3F))

            is Barrel -> UserInterfaceUtility.centerInventoryTitle(Format.mm("<white>Barrel</white>"))
            else -> return
        }

        event.titleOverride(text)
    }
}