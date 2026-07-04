package xyz.devcmb.tumblers.util.item

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

open class AdvancedItemStack(val material: Material, val init: AdvancedItemStackContext.() -> Unit) {
    constructor(itemStack: ItemStack, init: AdvancedItemStackContext.() -> Unit = {}): this(itemStack.type, {
        val meta = itemStack.itemMeta
        this.item.itemMeta = meta
        count(itemStack.amount)

        init()
    })

    val context: AdvancedItemStackContext = AdvancedItemStackContext(material)
    init {
        context.init()
    }

    fun build(): ItemStack {
        return context.build()
    }
}