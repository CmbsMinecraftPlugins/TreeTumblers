package xyz.devcmb.tumblers.controllers.games.crumble.kits

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.controllers.games.crumble.Kit
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.tickSeconds

class WarriorKit(
    override val player: TumblingPlayer?,
    override val crumble: CrumbleController
) : Kit {
    val blindnessTicks: Long = configurable("games.crumble.kits.warrior.blindness_ticks")

    override val id: String = "warrior"
    override val name: String = "Warrior"
    override val inventoryModel: NamespacedKey = NamespacedKey(TreeTumblers.NAMESPACE, "crumble/warrior")
    override val items: ArrayList<ItemStack> = arrayListOf(
        ItemStack(Material.STONE_SWORD),
        ItemStack(Material.STONE_PICKAXE),
        ItemStack(Material.LEATHER_BOOTS)
    )

    override val abilityName: String = "Eyelid Exterminator"
    override val abilityDescription: String = "Revoke your opponents ability to see. Gives blindness to the next person you hit for ${blindnessTicks.tickSeconds}s or until they're hit"
    override val killPowerName: String = "Strength"
    override val killPowerDescription: String = "Enchants your sword with sharpness for 1 hit"

    override val kitIcon: String = "\uE006"
    override val kitDisplayTextLength: Double = 50.5

    override fun onKill(killed: Player) {
        require(player != null) { "Cannot invoke methods on the kit template" }
        require(player.isOnline) { "Player must be online to invoke methods on the kit" }

        val sword = player.bukkitPlayer!!.inventory.first { it.type == items[0].type }
        sword.addEnchantment(Enchantment.SHARPNESS, 1)
    }

    var abilityActive = false
    override fun onAbility() {
        require(player != null) { "Cannot invoke methods on the kit template" }
        require(player.isOnline) { "Player must be online to invoke methods on the kit" }

        val sword = player.bukkitPlayer!!.inventory.first { it.type == items[0].type }
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
            || player?.isOnline != true
            || damager != player.bukkitPlayer!!
        ) return

        val sword = damager.inventory.itemInMainHand
        if(sword.type != items[0].type) return

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