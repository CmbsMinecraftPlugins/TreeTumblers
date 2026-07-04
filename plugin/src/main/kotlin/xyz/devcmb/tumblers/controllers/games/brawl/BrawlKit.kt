package xyz.devcmb.tumblers.controllers.games.brawl

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import xyz.devcmb.tumblers.util.Kit
import xyz.devcmb.tumblers.util.splashPotion

val sharedKitItems = arrayListOf(
    Kit.KitItem.StandardItem(ItemStack(Material.IRON_PICKAXE), null, false),
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
            Kit.KitItem.StandardItem(ItemStack(Material.STONE_SWORD), null, false),
            Kit.KitItem.StandardItem(ItemStack(Material.GOLDEN_APPLE, 2)),
            *sharedKitItems,
        )
        override val defaultDroppability: Boolean = true
    }),
    HEALER("Healer", object : Kit.KitDefinition {
        override val items: ArrayList<Kit.KitItem> = arrayListOf(
            Kit.KitItem.StandardItem(ItemStack(Material.WOODEN_AXE), null, false),
            Kit.KitItem.StandardItem(regenerationSplashPotion.clone(), null, false),
            Kit.KitItem.StandardItem(regenerationSplashPotion.clone(), null, false),
            Kit.KitItem.StandardItem(regenerationSplashPotion.clone(), null, false),
            *sharedKitItems,
        )
        override val defaultDroppability: Boolean = true
    }),
    NINJA("Ninja", object : Kit.KitDefinition {
        override val items: ArrayList<Kit.KitItem> = arrayListOf(
            Kit.KitItem.StandardItem(ItemStack(Material.STONE_SWORD), null, false),
            // TODO: Add scrolls
            *sharedKitItems
        )
        override val defaultDroppability: Boolean = true
    });
}