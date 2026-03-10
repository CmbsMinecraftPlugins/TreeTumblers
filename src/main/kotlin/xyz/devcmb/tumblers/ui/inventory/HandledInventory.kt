package xyz.devcmb.tumblers.ui.inventory

import xyz.devcmb.invcontrol.chest.ChestInventoryUI

interface HandledInventory {
    val id: String
    val inventory: ChestInventoryUI
}