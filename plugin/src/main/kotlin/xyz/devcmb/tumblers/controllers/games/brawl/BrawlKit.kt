package xyz.devcmb.tumblers.controllers.games.brawl

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.devcmb.tumblers.item.custom.scroll.ScrollItem
import xyz.devcmb.tumblers.util.Kit
import xyz.devcmb.tumblers.util.splashPotion
import java.util.UUID

val sharedKitItems = arrayListOf(
    Kit.KitItem.StandardItem(ItemStack(Material.IRON_PICKAXE).apply {
        editMeta {
            addEnchantment(Enchantment.EFFICIENCY, 3)
        }
    }, false),
    Kit.KitItem.StandardItem(ItemStack(Material.COOKED_BEEF, 8)),
    Kit.KitItem.TeamConcreteItem(false),

    Kit.KitItem.ArmorItem(ItemStack(Material.IRON_CHESTPLATE)),
    Kit.KitItem.ArmorItem(ItemStack(Material.LEATHER_LEGGINGS)),
    Kit.KitItem.ArmorItem(ItemStack(Material.LEATHER_BOOTS)),
).toTypedArray()

val regenerationEffect = PotionEffect(PotionEffectType.REGENERATION, 4 * 20, 1)
val regenerationSplashPotion = regenerationEffect.splashPotion("Splash Potion of Regeneration II")

enum class BrawlKit(val kitName: String, val kit: Kit.KitDefinition) {
    WARRIOR("Warrior", object : Kit.KitDefinition {
        override val items: ArrayList<Kit.KitItem> = arrayListOf(
            Kit.KitItem.StandardItem(ItemStack(Material.STONE_SWORD), false),
            Kit.KitItem.StandardItem(ItemStack(Material.GOLDEN_APPLE, 2)),
            *sharedKitItems,
        )
        override val defaultDropability: Boolean = true
        override val uuid: UUID = UUID.randomUUID()
    }),
    HEALER("Healer", object : Kit.KitDefinition {
        override val items: ArrayList<Kit.KitItem> = arrayListOf(
            Kit.KitItem.StandardItem(ItemStack(Material.WOODEN_AXE), false),
            Kit.KitItem.StandardItem(regenerationSplashPotion.clone(), false),
            Kit.KitItem.StandardItem(regenerationSplashPotion.clone(), false),
            Kit.KitItem.AdvancedItem(ScrollItem(ScrollItem.ScrollEffect.REGENERATION).build()),
            *sharedKitItems,
        )
        override val defaultDropability: Boolean = true
        override val uuid: UUID = UUID.randomUUID()
    }),
    NINJA("Ninja", object : Kit.KitDefinition {
        override val items: ArrayList<Kit.KitItem> = arrayListOf(
            Kit.KitItem.StandardItem(ItemStack(Material.STONE_SWORD), false),
            Kit.KitItem.AdvancedItem(ScrollItem(ScrollItem.ScrollEffect.INVISIBILITY).build().apply {
                this.context.count(2)
            }),
            Kit.KitItem.AdvancedItem(ScrollItem(ScrollItem.ScrollEffect.JUMP_BOOST).build()),
            Kit.KitItem.AdvancedItem(ScrollItem(ScrollItem.ScrollEffect.SPEED).build()),
            *sharedKitItems
        )
        override val defaultDropability: Boolean = true
        override val uuid: UUID = UUID.randomUUID()
    }),
    BRUTE("Brute", object : Kit.KitDefinition {
        override val items: ArrayList<Kit.KitItem> = arrayListOf(
            Kit.KitItem.StandardItem(ItemStack(Material.STONE_SWORD)),
            Kit.KitItem.StandardItem(ItemStack(Material.STONE_AXE)),
            *sharedKitItems,
        )
        override val defaultDropability: Boolean = true
        override val uuid: UUID = UUID.randomUUID()
    });
}