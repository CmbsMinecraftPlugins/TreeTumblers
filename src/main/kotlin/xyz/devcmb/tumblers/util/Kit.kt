package xyz.devcmb.tumblers.util

import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import xyz.devcmb.tumblers.util.item.AdvancedItemStack
import xyz.devcmb.tumblers.util.item.AdvancedItemStackContext

object Kit {
    interface KitDefinition {
        val items: ArrayList<KitItem>
        val defaultDroppability: Boolean
            get() = false
    }

    sealed interface KitItem {
        fun give(kit: KitDefinition, player: Player)

        class AdvancedItem(
            material: Material,
            val slot: Int? = null,
            init: AdvancedItemStackContext.() -> Unit
        ): AdvancedItemStack(material, init), KitItem {
            override fun give(kit: KitDefinition, player: Player) {
                if(!context.droppableChanged) context.droppable(kit.defaultDroppability)
                val item = build()

                if(slot != null) player.inventory.setItem(slot, item)
                else player.inventory.addItem(item)
            }
        }

        class StandardItem(
            val itemStack: ItemStack,
            val slot: Int? = null,
        ): KitItem {
            override fun give(kit: KitDefinition, player: Player) {
                val item = AdvancedItemStack(itemStack.clone()) {
                    droppable(kit.defaultDroppability)
                }.build()

                if(slot != null) player.inventory.setItem(slot, item)
                else player.inventory.addItem(item)
            }
        }

        class ArmorItem(val itemStack: ItemStack): KitItem {
            override fun give(kit: KitDefinition, player: Player) {
                val item = AdvancedItemStack(itemStack.clone()) {
                    droppable(kit.defaultDroppability)
                }.build()

                if(item.type.name.contains("LEATHER")) {
                    item.itemMeta = item.itemMeta.also { meta ->
                        val meta = meta as LeatherArmorMeta
                        val playerTeam = player.tumblingPlayer.team
                        meta.setColor(Color.fromRGB(playerTeam.color.value()))
                    }
                }

                player.inventory.setItem(item.type.equipmentSlot, item)
            }
        }
    }

    fun giveKit(player: Player, kit: KitDefinition) {
        player.inventory.clear()
        kit.items.forEach {
            it.give(kit, player)
        }
    }

    fun giveKits(players: Set<Player>, kit: KitDefinition) {
        players.forEach {
            giveKit(it, kit)
        }
    }
}