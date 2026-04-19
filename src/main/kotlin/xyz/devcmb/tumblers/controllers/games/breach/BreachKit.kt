package xyz.devcmb.tumblers.controllers.games.breach

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.Kit

enum class BreachKit(val label: Component, val description: List<Component>, val kit: Kit.KitDefinition, val item: Material) {
    BOW(
        Format.mm("Bow"),
        listOf(
            Format.mm("<white>Charge up as much as you wish."),
            Format.mm("<white>Comes with <yellow>2 arrows.</yellow>")
        ),
        object : Kit.KitDefinition {
            override val items: ArrayList<ItemStack> = arrayListOf(
                ItemStack(Material.BOW).apply {
                    itemMeta = itemMeta.also {
                        it.isUnbreakable = true
                    }
                },
                ItemStack(Material.ARROW, 2)
            )
            override val teamArmorSlot: EquipmentSlot? = EquipmentSlot.FEET
        },
        Material.BOW
    ),
    CROSSBOW(
        Format.mm("Crossbow"),
        listOf(
            Format.mm("<white>Store a charge, fire whenever needed."),
            Format.mm("<white>Comes with <yellow>1 arrow.</yellow>")
        ),
        object : Kit.KitDefinition {
            override val items: ArrayList<ItemStack> = arrayListOf(
                ItemStack(Material.CROSSBOW).apply {
                    itemMeta = itemMeta.also {
                        it.isUnbreakable = true
                    }
                },
                ItemStack(Material.ARROW, 1)
            )
            override val teamArmorSlot: EquipmentSlot? = EquipmentSlot.FEET
        },
        Material.CROSSBOW
    ),
    TRIDENT(
        Format.mm("<white>Trident</white>"),
        listOf(
            Format.mm("<white>Works as both melee and ranged."),
            Format.mm("<white>Remember, you'll need to <aqua>pick it</aqua>"),
            Format.mm("<aqua>up yourself after.</aqua>")
        ),
        object : Kit.KitDefinition {
            override val items: ArrayList<ItemStack> = arrayListOf(
                ItemStack(Material.TRIDENT).apply {
                    itemMeta = itemMeta.also {
                        it.isUnbreakable = true
                    }
                }
            )
            override val teamArmorSlot: EquipmentSlot? = EquipmentSlot.FEET
        },
        Material.TRIDENT
    )
}