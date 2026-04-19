package xyz.devcmb.tumblers.util.item

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class AdvancedItemStack(val material: Material, val init: AdvancedItemStackContext.() -> Unit) {
    constructor(itemStack: ItemStack, init: AdvancedItemStackContext.() -> Unit = {}): this(itemStack.type, {
        val meta = itemStack.itemMeta

        if(meta.hasItemName()) name(meta.itemName())
        if(meta.hasLore()) lore(meta.lore()!!)
        if(meta.hasItemModel()) model(meta.itemModel!!)
        if(meta.hasEnchants()) enchants(meta.enchants)
        unbreakable(meta.isUnbreakable)

        count(itemStack.amount)

        persistentDataContainer {
            meta.persistentDataContainer.copyTo(this, true)
        }

        init()
    })

    fun build(): ItemStack {
        val context = AdvancedItemStackContext(material)
        context.init()
        return context.build()
    }
}