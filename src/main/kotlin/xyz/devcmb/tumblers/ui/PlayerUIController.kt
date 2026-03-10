package xyz.devcmb.tumblers.ui

import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.ui.inventory.crumble.CrumbleKitSelector

class PlayerUIController(val player: Player) {
    val inventories: ArrayList<HandledInventory> = ArrayList()
    val gameController = ControllerDelegate.getController("gameController") as GameController

    init {
        registerInventory(CrumbleKitSelector(player, gameController))
    }

    fun registerInventory(inv: HandledInventory) {
        inventories.add(inv)
    }

    fun openInventory(id: String) {
        val handledInventory = inventories.find { it.id == id }
        if(handledInventory == null) throw IllegalArgumentException("Inventory with an id of $id does not exist")

        handledInventory.inventory.show()
    }
}