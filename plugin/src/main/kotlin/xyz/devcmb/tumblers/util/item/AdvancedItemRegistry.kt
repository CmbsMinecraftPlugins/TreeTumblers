package xyz.devcmb.tumblers.util.item

import org.bukkit.NamespacedKey
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.util.runTask

object AdvancedItemRegistry {
    val items: HashMap<String, AdvancedItemStackContext> = HashMap()
    val key = NamespacedKey(TreeTumblers.NAMESPACE, "advanced_item")

    fun register(stack: AdvancedItemStackContext) {
        items[stack.id] = stack
    }

    fun handleInteract(event: PlayerInteractEvent) {
        val stack = event.item ?: return
        val item = getItem(stack) ?: return

        when (event.action) {
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK ->
                item.rightClick?.invoke(stack, event.player)

            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK ->
                item.leftClick?.invoke(stack, event.player)

            else -> {}
        }
    }

    fun handleInventoryClickEvent(event: InventoryClickEvent) {
        val itemsToCheck: ArrayList<ItemStack> = ArrayList()

        if(event.hotbarButton != -1) {
            val hotbarItem = event.whoClicked.inventory.getItem(event.hotbarButton)
            if(hotbarItem != null) itemsToCheck.add(hotbarItem)
        }

        val stacks = listOfNotNull(event.currentItem, event.cursor)
        itemsToCheck.addAll(stacks)

        val items = itemsToCheck.mapNotNull { getItem(it) }

        items.forEach {
            if(!it.movable) {
                event.isCancelled = true
            }
        }
    }

    fun handleBlockPlace(event: BlockPlaceEvent) {
        val stack = event.itemInHand
        val item = getItem(stack) ?: return

        if(item.returnOnPlace) {
            runTask {
                stack.amount += 1
            }
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