package xyz.devcmb.tumblers.controllers.games.breach

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.item.Kit
import java.util.UUID

enum class BreachKit(val label: Component, val description: List<Component>, val kit: Kit.KitDefinition, val item: Material) {
    BOW(
        Format.mm("Bow"),
        listOf(
            Format.mm("<white>Charge up as much as you wish."),
        ),
        object : Kit.KitDefinition {
            override val items: ArrayList<Kit.KitItem> = arrayListOf(
                Kit.KitItem.StandardItem(ItemStack(Material.BOW).apply {
                    itemMeta = itemMeta.also {
                        it.isUnbreakable = true
                        it.addEnchant(Enchantment.INFINITY, 1, true)
                    }
                }),
                Kit.KitItem.StandardItem(ItemStack(Material.ARROW, 1)),
                Kit.KitItem.ArmorItem(ItemStack(Material.LEATHER_BOOTS))
            )
            override val uuid: UUID = UUID.randomUUID()
        },
        Material.BOW
    ),
    CROSSBOW(
        Format.mm("Crossbow"),
        listOf(
            Format.mm("<white>Store a charge for later, fire instantly.")
        ),
        object : Kit.KitDefinition {
            override val items: ArrayList<Kit.KitItem> = arrayListOf(
                Kit.KitItem.StandardItem(ItemStack(Material.CROSSBOW).apply {
                    itemMeta = itemMeta.also {
                        it.isUnbreakable = true
                        it.addEnchant(Enchantment.INFINITY, 1, true)
                    }
                }),
                Kit.KitItem.StandardItem(ItemStack(Material.ARROW, 1)),
                Kit.KitItem.ArmorItem(ItemStack(Material.LEATHER_BOOTS))
            )
            override val uuid: UUID = UUID.randomUUID()
        },
        Material.CROSSBOW
    ),
    TRIDENT(
        Format.mm("<white>Trident</white>"),
        listOf(
            Format.mm("<white>Works as both melee and ranged.")
        ),
        object : Kit.KitDefinition {
            override val items: ArrayList<Kit.KitItem> = arrayListOf(
                Kit.KitItem.StandardItem(ItemStack(Material.TRIDENT).apply {
                    itemMeta = itemMeta.also {
                        it.isUnbreakable = true
                        it.addEnchant(Enchantment.LOYALTY, 1, true)
                    }
                }),
                Kit.KitItem.ArmorItem(ItemStack(Material.LEATHER_BOOTS))
            )
            override val uuid: UUID = UUID.randomUUID()
        },
        Material.TRIDENT
    )
}