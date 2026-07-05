package xyz.devcmb.tumblers.util

import org.bukkit.Color
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import xyz.devcmb.tumblers.util.item.AdvancedItemStack

object Kit {
    interface KitDefinition {
        val items: ArrayList<KitItem>
        val defaultDropability: Boolean
            get() = false
    }

    sealed interface KitItem {
        fun give(kit: KitDefinition, player: Player)

        class AdvancedItem(
            val stack: AdvancedItemStack,
            val slot: Int? = null,
            val droppableOverride: Boolean? = null,
        ): KitItem {
            override fun give(kit: KitDefinition, player: Player) {
                val context = stack.context
                if(!context.droppableChanged) context.droppable(droppableOverride ?: kit.defaultDropability)
                val item = stack.build()

                if(slot != null) player.inventory.setItem(slot, item)
                else player.inventory.addItem(item)
            }
        }

        class StandardItem(
            val itemStack: ItemStack,
            val slot: Int? = null,
            val droppableOverride: Boolean? = null
        ): KitItem {
            override fun give(kit: KitDefinition, player: Player) {
                val item = AdvancedItemStack(itemStack.clone()) {
                    droppable(droppableOverride ?: kit.defaultDropability)
                }.build()

                if(slot != null) player.inventory.setItem(slot, item)
                else player.inventory.addItem(item)
            }
        }

        class ArmorItem(val itemStack: ItemStack): KitItem {
            override fun give(kit: KitDefinition, player: Player) {
                val item = AdvancedItemStack(itemStack.clone()) {
                    droppable(kit.defaultDropability)
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

        class TeamConcreteItem(val droppableOverride: Boolean?) : KitItem {
            override fun give(kit: KitDefinition, player: Player) {
                val item = AdvancedItemStack(ItemStack.of(player.tumblingPlayer.team.concrete)) {
                    returnOnPlace = true
                    droppable(droppableOverride ?: kit.defaultDropability)
                    count(64)
                }.build()
                player.inventory.addItem(item)
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