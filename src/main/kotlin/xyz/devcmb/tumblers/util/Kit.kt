package xyz.devcmb.tumblers.util

import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ColorableArmorMeta

object Kit {
    interface KitDefinition {
        val items: ArrayList<ItemStack>
        val teamArmorSlot: EquipmentSlot?
    }

    val leatherItems: HashMap<EquipmentSlot, ItemStack> = hashMapOf(
        EquipmentSlot.HEAD to ItemStack.of(Material.LEATHER_HELMET),
        EquipmentSlot.CHEST to ItemStack.of(Material.LEATHER_CHESTPLATE),
        EquipmentSlot.LEGS to ItemStack.of(Material.LEATHER_LEGGINGS),
        EquipmentSlot.FEET to ItemStack.of(Material.LEATHER_BOOTS)
    )

    fun giveKit(player: Player, kit: KitDefinition) {
        player.inventory.clear()
        player.inventory.addItem(*kit.items.map { it.clone() }.toTypedArray())

        if(kit.teamArmorSlot != null) {
            player.inventory.setItem(kit.teamArmorSlot!!, leatherItems[kit.teamArmorSlot]!!.clone().apply {
                itemMeta = (itemMeta as ColorableArmorMeta).also {
                    it.setColor(Color.fromRGB(player.tumblingPlayer.team.color.value()))
                }
            })
        }
    }

    fun giveKit(player: Player, items: ArrayList<ItemStack>, teamArmorSlot: EquipmentSlot?) =
        giveKit(player, object : KitDefinition {
            override val items: ArrayList<ItemStack> = items
            override val teamArmorSlot: EquipmentSlot? = teamArmorSlot
        })

    fun giveKits(players: Set<Player>, kit: KitDefinition) {
        players.forEach {
            giveKit(it, kit)
        }
    }
}