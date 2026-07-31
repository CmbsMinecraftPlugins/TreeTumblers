package xyz.devcmb.tumblers.controllers.games.crumble.kits

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleKit
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.item.Kit
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.tickSeconds
import xyz.devcmb.tumblers.util.tumblingPlayer
import java.util.UUID

class SorcererKit(
    override val companion: CrumbleKit.Companion,
    override val player: TumblingPlayer,
    override val crumble: CrumbleController
) : CrumbleKit {
    companion object : CrumbleKit.Companion {
        val poisonDuration: Long = configurable("games.crumble.kits.sorcerer.poison_duration")
        val healHearts: Int = configurable("games.crumble.kits.sorcerer.heal_hearts")
        val healDuration: Long = configurable("games.crumble.kits.sorcerer.heal_duration")

        override val id: String = "sorcerer"
        override val name: String = "Sorcerer"
        override val kit: Kit.KitDefinition = object : Kit.KitDefinition {
            override val items: ArrayList<Kit.KitItem> = arrayListOf(
                Kit.KitItem.StandardItem(ItemStack(Material.STONE_SWORD)),
                Kit.KitItem.StandardItem(ItemStack(Material.STONE_PICKAXE)),
                Kit.KitItem.ArmorItem(ItemStack(Material.LEATHER_BOOTS)),
            )
            override val uuid: UUID = UUID.randomUUID()
        }

        override val abilityName: String = "Poison Haze"
        override val abilityDescription: String = "Turns your sword into a nail of poison. Hitting anyone will give them the poison effect for ${poisonDuration.tickSeconds}s"
        override val killPowerName: String = "Kill With Kindness"
        override val killPowerDescription: String = "Heals you $healHearts hearts over ${healDuration.tickSeconds}s"

        override fun create(
            player: TumblingPlayer,
            crumble: CrumbleController
        ): CrumbleKit = SorcererKit(this, player, crumble)
    }

    override fun onKill(killed: Player) {
        object : BukkitRunnable() {
            var heals: Int = 0
            override fun run() {
                player.bukkitPlayer!!.heal(healHearts / 20.0)
                heals++

                if(heals > healDuration) {
                    cancel()
                }
            }
        }.runTaskTimer(TreeTumblers.plugin, 0, 1)
    }

    var abilityActive = false
    override fun onAbility() {
        val sword = player.bukkitPlayer!!.inventory.first { it.type == WarriorKit.kit.items.first().getStack(WarriorKit.kit, player.bukkitPlayer!!).type }
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
            || damager != player.bukkitPlayer
            || damager.tumblingPlayer.team == player.team
            || !abilityActive
        ) return

        val sword = damager.inventory.itemInMainHand
        if(sword.type != WarriorKit.kit.items.first().getStack(WarriorKit.kit, player.bukkitPlayer!!).type) return

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