package xyz.devcmb.tumblers.util

import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ColorableArmorMeta
import xyz.devcmb.tumblers.util.item.AdvancedItemStack

object Kit {
    interface KitDefinition {
        val items: ArrayList<ItemStack>
        val allowItemDrops: Boolean
            get() = false
        val teamArmorSlot: EquipmentSlot?
    }

    val leatherItems: HashMap<EquipmentSlot, AdvancedItemStack> = hashMapOf(
        EquipmentSlot.HEAD to AdvancedItemStack(Material.LEATHER_HELMET) { droppable(false) },
        EquipmentSlot.CHEST to AdvancedItemStack(Material.LEATHER_CHESTPLATE) { droppable(false) },
        EquipmentSlot.LEGS to AdvancedItemStack(Material.LEATHER_LEGGINGS) { droppable(false) },
        EquipmentSlot.FEET to AdvancedItemStack(Material.LEATHER_BOOTS) { droppable(false) }
    )

    fun giveKit(player: Player, kit: KitDefinition) {
        val items = kit.items.map {
            AdvancedItemStack(it.clone()) {
                if(!kit.allowItemDrops) droppable(false)
            }.build()
        }

        player.inventory.clear()
        player.inventory.addItem(*items.toTypedArray())

        if(kit.teamArmorSlot != null) {
            player.inventory.setItem(kit.teamArmorSlot!!, leatherItems[kit.teamArmorSlot]!!.build().clone().apply {
                itemMeta = (itemMeta as ColorableArmorMeta).also {
                    it.setColor(Color.fromRGB(player.tumblingPlayer.team.color.value()))
                }
            })
        }
    }

    fun giveKits(players: Set<Player>, kit: KitDefinition) {
        players.forEach {
            giveKit(it, kit)
        }
    }
}