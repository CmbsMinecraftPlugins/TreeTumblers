package xyz.devcmb.tumblers.util.item

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

class AdvancedItemStackContext(
    material: Material
) {
    val id = UUID.randomUUID().toString()
    private val item = ItemStack(material)

    var rightClick: ((Player) -> Unit)? = null
    var leftClick: ((Player) -> Unit)? = null

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

    fun rightClick(action: (Player) -> Unit) {
        rightClick = action
    }

    fun leftClick(action: (Player) -> Unit) {
        leftClick = action
    }

    fun build(): ItemStack {
        val meta = item.itemMeta

        meta.persistentDataContainer.set(
            AdvancedItemRegistry.key,
            PersistentDataType.STRING,
            id
        )

        item.itemMeta = meta

        AdvancedItemRegistry.register(this)
        return item
    }
}