package xyz.devcmb.tumblers.util.item

import org.bukkit.NamespacedKey
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object AdvancedItemRegistry {
    val items: HashMap<String, AdvancedItemStackContext> = HashMap()
    val key = NamespacedKey("tumbling", "advanced_item")

    fun register(stack: AdvancedItemStackContext) {
        items.put(stack.id, stack)
    }

    fun handleInteract(event: PlayerInteractEvent) {
        val stack = event.item ?: return
        val item = getItem(stack) ?: return

        when (event.action) {
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK ->
                item.rightClick?.invoke(event.player)

            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK ->
                item.leftClick?.invoke(event.player)

            else -> {}
        }
    }

    fun handleDrop(event: PlayerDropItemEvent) {
        val item = getItem(event.itemDrop.itemStack) ?: return
        if(!item.droppable) event.isCancelled = true
    }

    private fun getItem(item: ItemStack): AdvancedItemStackContext? {
        val meta = item.itemMeta ?: return null

        val id = meta.persistentDataContainer.get(
            key,
            PersistentDataType.STRING
        ) ?: return null

        return items[id]
    }
}