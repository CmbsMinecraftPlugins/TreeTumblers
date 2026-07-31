package xyz.devcmb.tumblers.controllers.games.crumble.kits

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleKit
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.item.Kit
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.tickSeconds
import java.util.UUID

class WarriorKit(
    override val companion: CrumbleKit.Companion,
    override val player: TumblingPlayer,
    override val crumble: CrumbleController
) : CrumbleKit {
    companion object : CrumbleKit.Companion {
        val blindnessTicks: Long = configurable("games.crumble.kits.warrior.blindness_ticks")

        override val id: String = "warrior"
        override val name: String = "Warrior"
        override val kit: Kit.KitDefinition = object : Kit.KitDefinition {
            override val items: ArrayList<Kit.KitItem> = arrayListOf(
                Kit.KitItem.StandardItem(ItemStack(Material.STONE_SWORD)),
                Kit.KitItem.StandardItem(ItemStack(Material.STONE_PICKAXE)),
                Kit.KitItem.ArmorItem(ItemStack(Material.LEATHER_BOOTS))
            )
            override val uuid: UUID = UUID.randomUUID()
        }

        override val abilityName: String = "Eyelid Exterminator"
        override val abilityDescription: String = "Revoke your opponents ability to see. Gives blindness to the next person you hit for ${blindnessTicks.tickSeconds}s or until they're hit"
        override val killPowerName: String = "Strength"
        override val killPowerDescription: String = "Enchants your sword with sharpness for 1 hit"

        override fun create(
            player: TumblingPlayer,
            crumble: CrumbleController
        ): CrumbleKit = WarriorKit(this, player, crumble)
    }

    override fun onKill(killed: Player) {
        val sword = player.bukkitPlayer!!.inventory.first { it.type == kit.items.first().getStack(kit, player.bukkitPlayer!!).type }
        sword.addEnchantment(Enchantment.SHARPNESS, 1)
    }

    var abilityActive = false
    override fun onAbility() {
        val sword = player.bukkitPlayer!!.inventory.first { it.type == Material.STONE_SWORD }
        sword.itemMeta = sword.itemMeta.also {
            it.setEnchantmentGlintOverride(true)
            it.lore(arrayListOf(
                Component.text("Eyelid Exterminator", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)
            ))
        }
        abilityActive = true
    }

    @EventHandler
    fun playerAbilityEvent(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val damaged = event.entity

        if(
            damager !is Player
            || damaged !is Player
            || damager != player.bukkitPlayer
        ) return

        val sword = damager.inventory.itemInMainHand
        if(sword.type != kit.items.first().getStack(kit, player.bukkitPlayer!!).type) return

        sword.removeEnchantment(Enchantment.SHARPNESS)

        if(!abilityActive) return

        sword.itemMeta = sword.itemMeta.also {
            it.setEnchantmentGlintOverride(null)
            it.lore(arrayListOf())
        }
        damaged.addPotionEffect(PotionEffect(
            PotionEffectType.BLINDNESS,
            blindnessTicks.toInt(),
            1,
            false,
            true,
            true
        ))
        abilityActive = false
    }
}