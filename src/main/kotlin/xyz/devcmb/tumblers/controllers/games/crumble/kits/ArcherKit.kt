package xyz.devcmb.tumblers.controllers.games.crumble.kits

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.controllers.games.crumble.Kit
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.seconds

class ArcherKit(
    override val player: Player?,
    override val crumble: CrumbleController,
) : Kit {
    override val id: String = "archer"
    override val name: String = "Archer"
    override val inventoryModel: NamespacedKey = NamespacedKey("tumbling", "crumble/archer")
    override val items: ArrayList<ItemStack> = arrayListOf(
        ItemStack(Material.WOODEN_SWORD).apply {
            addEnchantment(Enchantment.KNOCKBACK, 1)
        },
        ItemStack(Material.BOW).apply {
            itemMeta = itemMeta.also {
                it.isUnbreakable = true
            }
        },
        ItemStack(Material.ARROW, 4),
        ItemStack(Material.LEATHER_HELMET)
    )

    override val abilityName: String = "Sniper"
    override val abilityDescription: String =
        "Enchants your bow with power ${MiscUtils.intToRoman(powerLevel)} and punch ${MiscUtils.intToRoman(punchLevel)}! Can only be fired once."
    override val killPowerName: String = "Robin Hood"
    override val killPowerDescription: String = "Gives swiftness for ${swiftnessTicks.seconds}s"

    override val kitIcon: String = "\uE000"
    override val kitDisplayTextLength: Int = 49

    companion object {
        @field:Configurable("games.crumble.kits.archer.power_level")
        var powerLevel: Int = 3

        @field:Configurable("games.crumble.kits.archer.punch_level")
        var punchLevel: Int = 2

        @field:Configurable("games.crumble.kits.archer.swiftness_ticks")
        var swiftnessTicks: Long = 30
    }

    var abilityActive = false

    override fun onKill(killed: Player) {
        require(player != null) { "Cannot invoke methods on the kit template" }
        player.addPotionEffect(PotionEffect(
            PotionEffectType.SPEED,
            swiftnessTicks.toInt(),
            1,
            false,
            true
        ))
    }

    override fun onAbility() {
        require(player != null) { "Cannot invoke methods on the kit template" }
        val bow = player.inventory.first { it.type == Material.BOW }!!
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
            || player != shooter
            || !abilityActive
        ) return

        val bow = player.inventory.first { it.type == Material.BOW } ?: return
        bow.removeEnchantments()
        abilityActive = false
    }

    override fun cleanup() {
        abilityActive = false
    }
}