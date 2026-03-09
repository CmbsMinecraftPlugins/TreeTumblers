package xyz.devcmb.tumblers.util.item

import org.bukkit.NamespacedKey
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType

object AdvancedItemRegistry {
    val items: HashMap<String, AdvancedItemStackContext> = HashMap()
    val key = NamespacedKey("tumbling", "advanced_item")

    fun register(stack: AdvancedItemStackContext) {
        items.put(stack.id, stack)
    }

    fun handleInteract(event: PlayerInteractEvent) {
        val item = event.item ?: return
        val meta = item.itemMeta ?: return

        val id = meta.persistentDataContainer.get(
            key,
            PersistentDataType.STRING
        ) ?: return

        val data = items.get(id) ?: return

        when (event.action) {
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK ->
                data.rightClick?.invoke(event.player)

            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK ->
                data.leftClick?.invoke(event.player)

            else -> {}
        }
    }
}