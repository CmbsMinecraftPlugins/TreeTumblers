package xyz.devcmb.tumblers.controllers.games.crumble.kits

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.controllers.games.crumble.Kit
import xyz.devcmb.tumblers.util.tickSeconds

class SorcererKit(
    override val player: Player?,
    override val crumble: CrumbleController
) : Kit {
    override val id: String = "sorcerer"
    override val name: String = "Sorcerer"
    override val inventoryModel: NamespacedKey = NamespacedKey("tumbling", "crumble/sorcerer")
    override val items: ArrayList<ItemStack> = arrayListOf(
        ItemStack(Material.STONE_SWORD),
        ItemStack(Material.STONE_PICKAXE),
        ItemStack(Material.LEATHER_BOOTS)
    )

    override val abilityName: String = "Poison Haze"
    override val abilityDescription: String = "Turns your sword into a nail of poison. Hitting anyone will give them the poison effect for ${poisonDuration.tickSeconds}s"
    override val killPowerName: String = "Kill With Kindness"
    override val killPowerDescription: String = "Heals you $healHearts hearts over ${healDuration.tickSeconds}s"

    override val kitIcon: String = "\uE005"
    override val kitDisplayTextLength: Double = 59.75

    companion object {
        @field:Configurable("games.crumble.kits.sorcerer.poison_duration")
        var poisonDuration: Long = 70

        @field:Configurable("games.crumble.kits.sorcerer.heal_hearts")
        var healHearts: Int = 2

        @field:Configurable("games.crumble.kits.sorcerer.heal_duration")
        var healDuration: Long = 60
    }

    override fun onKill(killed: Player) {
        require(player != null) { "Cannot invoke methods on the kit template" }
        object : BukkitRunnable() {
            var heals: Int = 0
            override fun run() {
                player.heal(healHearts / 20.0)
                heals++

                if(heals > healDuration) {
                    cancel()
                }
            }
        }.runTaskTimer(TreeTumblers.plugin, 0, 1)
    }

    var abilityActive = false
    override fun onAbility() {
        require(player != null) { "Cannot invoke methods on the kit template" }
        val sword = player.inventory.first { it.type == items[0].type }
        sword.itemMeta = sword.itemMeta.also {
            it.setEnchantmentGlintOverride(true)
            it.lore(arrayListOf(Component.text("Poison Haze", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)))
        }
        abilityActive = true
    }

    override fun cleanup() {
        abilityActive = false
    }

    @EventHandler
    fun playerAttackEvent(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val damaged = event.entity

        if(
            damager !is Player
            || damaged !is Player
            || damager != player
            || !abilityActive
        ) return

        val sword = damager.inventory.itemInMainHand
        if(sword.type != items[0].type) return
        sword.itemMeta = sword.itemMeta.also {
            it.setEnchantmentGlintOverride(null)
            it.lore(arrayListOf())
        }
        damaged.addPotionEffect(PotionEffect(
            PotionEffectType.POISON,
            poisonDuration.toInt(),
            1,
            false,
            true,
            true
        ))
        abilityActive = false
    }
}