package xyz.devcmb.tumblers.ui.inventory.brawl

import com.noxcrew.interfaces.interfaces.ChestInterface
import com.noxcrew.interfaces.interfaces.buildChestInterface
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.devcmb.tumblers.controllers.games.brawl.BrawlController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.Font

class BrawlKitSelector : HandledInventory {
    override val id: String = "brawlKitSelector"
    override val inventory: ChestInterface = buildChestInterface {
        titleSupplier = {
            UserInterfaceUtility.customInventoryTitle(
                Font.getGlyph("container/brawl_kit_selector"),
                Component.text("Kit Selector", NamedTextColor.WHITE)
            )
        }
        rows = 4
    }
}