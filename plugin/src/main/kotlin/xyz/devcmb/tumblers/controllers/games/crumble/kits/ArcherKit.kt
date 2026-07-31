package xyz.devcmb.tumblers.controllers.games.crumble.kits

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleKit
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.item.Kit
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.intToRoman
import xyz.devcmb.tumblers.util.tickSeconds
import java.util.UUID

class ArcherKit(
    override val companion: CrumbleKit.Companion,
    override val player: TumblingPlayer,
    override val crumble: CrumbleController,
) : CrumbleKit {
    companion object : CrumbleKit.Companion {
        val powerLevel: Int = configurable("games.crumble.kits.archer.power_level")
        val punchLevel: Int = configurable("games.crumble.kits.archer.punch_level")
        val swiftnessTicks: Long = configurable("games.crumble.kits.archer.swiftness_ticks")

        override val id: String = "archer"
        override val name: String = "Archer"

        override val kit: Kit.KitDefinition = object : Kit.KitDefinition {
            override val items: ArrayList<Kit.KitItem> = arrayListOf(
                Kit.KitItem.StandardItem(ItemStack(Material.WOODEN_SWORD).apply {
                    addEnchantment(Enchantment.KNOCKBACK, 1)
                }),
                Kit.KitItem.StandardItem(ItemStack(Material.STONE_PICKAXE)),
                Kit.KitItem.StandardItem(ItemStack(Material.BOW).apply {
                    editMeta {
                        it.isUnbreakable = true
                    }
                }),
                Kit.KitItem.StandardItem(ItemStack(Material.ARROW, 4)),
                Kit.KitItem.ArmorItem(ItemStack(Material.LEATHER_BOOTS))
            )
            override val uuid: UUID = UUID.randomUUID()
        }

        override val abilityName: String = "Sniper"
        override val abilityDescription: String =
            "Enchants your bow with power ${intToRoman(powerLevel)} and punch ${intToRoman(punchLevel)}! Can only be fired once."
        override val killPowerName: String = "Robin Hood"
        override val killPowerDescription: String = "Gives swiftness for ${swiftnessTicks.tickSeconds}s"

        override fun create(
            player: TumblingPlayer,
            crumble: CrumbleController
        ): CrumbleKit = ArcherKit(this, player, crumble)
    }

    var abilityActive = false

    override fun onKill(killed: Player) {
        player.bukkitPlayer!!.addPotionEffect(PotionEffect(
            PotionEffectType.SPEED,
            swiftnessTicks.toInt(),
            1,
            false,
            true
        ))
    }

    override fun onAbility() {
        val bow = player.bukkitPlayer!!.inventory.first { it.type == Material.BOW }!!
        bow.addEnchantments(mutableMapOf(
            Enchantment.PUNCH to punchLevel,
            Enchantment.POWER to powerLevel
        ))
        abilityActive = true
    }

    @EventHandler
    fun shootEvent(event: EntityShootBowEvent) {
        val entity = event.entity
        if(entity !is Arrow) return

        val shooter = entity.shooter
        if(
            shooter !is Player
            || player.bukkitPlayer != shooter
            || !abilityActive
        ) return

        val bow = player.bukkitPlayer!!.inventory.first { it.type == Material.BOW } ?: return
        bow.removeEnchantments()
        abilityActive = false
    }

    override fun cleanup() {
        abilityActive = false
    }
}