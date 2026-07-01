package xyz.devcmb.tumblers.ui.inventory.brawl

import com.noxcrew.interfaces.interfaces.ChestInterface
import com.noxcrew.interfaces.interfaces.buildChestInterface
import xyz.devcmb.tumblers.ui.inventory.HandledInventory

class BrawlKitSelector : HandledInventory {
    override val id: String = "brawlKitSelector"
    override val inventory: ChestInterface = buildChestInterface {
        rows = 4
        // TODO
    }
}