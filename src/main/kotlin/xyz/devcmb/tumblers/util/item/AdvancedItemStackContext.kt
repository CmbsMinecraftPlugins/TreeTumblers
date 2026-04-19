package xyz.devcmb.tumblers.util.item

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

class AdvancedItemStackContext(
    material: Material
) {
    val id = UUID.randomUUID().toString()
    val item = ItemStack(material)

    var rightClick: ((Player) -> Unit)? = null
    var leftClick: ((Player) -> Unit)? = null
    var droppable: Boolean = true

    fun name(component: Component) {
        item.itemMeta = item.itemMeta.also {
            it.itemName(component)
        }
    }

    fun lore(list: List<Component>) {
        item.itemMeta = item.itemMeta.also {
            it.lore(list)
        }
    }

    fun model(key: NamespacedKey) {
        item.itemMeta = item.itemMeta.also {
            it.itemModel = key
        }
    }

    fun count(amount: Int) {
        item.amount = amount
    }

    fun enchants(enchantments: Map<Enchantment, Int>) {
        item.itemMeta = item.itemMeta.also {
            enchantments.forEach { enchant ->
                it.addEnchant(enchant.key, enchant.value, true)
            }
        }
    }

    fun unbreakable(unbreakable: Boolean) {
        item.itemMeta = item.itemMeta.also {
            it.isUnbreakable = unbreakable
        }
    }

    fun rightClick(action: (Player) -> Unit) {
        rightClick = action
    }

    fun leftClick(action: (Player) -> Unit) {
        leftClick = action
    }

    fun droppable(bool: Boolean) {
        droppable = bool
    }

    fun persistentDataContainer(action: PersistentDataContainer.() -> Unit) {
        item.itemMeta = item.itemMeta.also {
            action(it.persistentDataContainer)
        }
    }

    fun build(): ItemStack {
        val meta = item.itemMeta

        meta.persistentDataContainer.set(
            AdvancedItemRegistry.key,
            PersistentDataType.STRING,
            id
        )

        item.itemMeta = meta

        if(Enchantment.BINDING_CURSE.canEnchantItem(item) && !droppable) {
            item.addEnchantment(Enchantment.BINDING_CURSE, 1)
        }

        AdvancedItemRegistry.register(this)
        return item
    }
}