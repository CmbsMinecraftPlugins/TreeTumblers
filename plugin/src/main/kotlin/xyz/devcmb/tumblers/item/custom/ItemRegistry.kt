package xyz.devcmb.tumblers.item.custom

import org.bukkit.entity.Player
import xyz.devcmb.tumblers.item.custom.scroll.ScrollItem
import kotlin.reflect.KClass

object ItemRegistry {
    val items: HashMap<String, KClass<out CustomItem>> = HashMap()

    fun registerItems() {
        items["scroll"] = ScrollItem::class
    }

    fun give(player: Player, item: String) {
        val itemDef = items[item] ?: throw IllegalArgumentException("Attempted to give an item that does not exist")
        val item = itemDef.constructors.first { it.parameters.isEmpty() }.call()

        player.inventory.addItem(item.build().build())
    }

    data class CustomItemDefinition(val id: String)
}