package xyz.devcmb.tumblers.ui.inventory

import com.noxcrew.interfaces.interfaces.ChestInterface

interface HandledInventory {
    val id: String
    val inventory: ChestInterface
}