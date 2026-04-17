package xyz.devcmb.tumblers.util.item

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class AdvancedItemStack(val material: Material, val init: AdvancedItemStackContext.() -> Unit) {
    val context: AdvancedItemStackContext = AdvancedItemStackContext(material)
    constructor(itemStack: ItemStack, init: AdvancedItemStackContext.() -> Unit = {}): this(itemStack.type, {
        val meta = itemStack.itemMeta

        if(meta.hasItemName()) name(meta.itemName())
        if(meta.hasLore()) lore(meta.lore()!!)
        if(meta.hasItemModel()) model(meta.itemModel!!)
        if(meta.hasEnchants()) enchants(meta.enchants)

        count(itemStack.amount)

        persistentDataContainer {
            meta.persistentDataContainer.copyTo(this, true)
        }

        init()
    })

    fun build(): ItemStack {
        context.init()
        return context.build()
    }
}