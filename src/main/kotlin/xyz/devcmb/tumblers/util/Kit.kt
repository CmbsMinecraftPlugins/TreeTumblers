package xyz.devcmb.tumblers.util

import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ColorableArmorMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import xyz.devcmb.tumblers.util.item.AdvancedItemStack

object Kit {
    interface KitDefinition {
        val items: ArrayList<ItemStack>
        val allowItemDrops: Boolean
            get() = false
    }

    fun giveKit(player: Player, kit: KitDefinition) {
        val items = kit.items.map {
            AdvancedItemStack(it.clone()) {
                droppable(kit.allowItemDrops)
            }.build()
        }

        player.inventory.clear()
        items.forEach { item ->
            if(isArmor(item)) {
                if(item.type.name.contains("LEATHER")) {
                    item.itemMeta = item.itemMeta.also { meta ->
                        val meta = meta as LeatherArmorMeta
                        val playerTeam = player.tumblingPlayer.team
                        meta.setColor(Color.fromRGB(playerTeam.color.value()))
                    }
                }

                player.inventory.setItem(item.type.equipmentSlot, item)
            } else {
                player.inventory.addItem(item)
            }
        }
    }

    fun giveKits(players: Set<Player>, kit: KitDefinition) {
        players.forEach {
            giveKit(it, kit)
        }
    }
}