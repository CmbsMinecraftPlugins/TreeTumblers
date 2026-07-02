package xyz.devcmb.tumblers.ui.inventory
import com.noxcrew.interfaces.interfaces.Interface

interface HandledInventory {
    val id: String
    val inventory: Interface<*, *>
}