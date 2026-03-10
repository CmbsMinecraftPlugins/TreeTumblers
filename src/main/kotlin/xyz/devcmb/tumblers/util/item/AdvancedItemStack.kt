package xyz.devcmb.tumblers.util.item

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class AdvancedItemStack(val material: Material, val init: AdvancedItemStackContext.() -> Unit) {
    fun build(): ItemStack {
        val context = AdvancedItemStackContext(material)
        context.init()
        return context.build()
    }
}