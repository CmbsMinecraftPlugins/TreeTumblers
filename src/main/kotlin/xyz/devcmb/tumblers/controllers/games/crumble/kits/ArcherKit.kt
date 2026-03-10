package xyz.devcmb.tumblers.controllers.games.crumble.kits

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.controllers.games.crumble.Kit
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.seconds

class ArcherKit(
    override val player: Player?,
) : Kit {
    override val inventoryModel: NamespacedKey = NamespacedKey("tumbling", "crumble/archer")
    override val id: String = "archer"
    override val name: String = "Archer"
    override val items: ArrayList<ItemStack> = arrayListOf(
        ItemStack(Material.WOODEN_SWORD).apply {
            addEnchantment(Enchantment.KNOCKBACK, 1)
        },
        ItemStack(Material.BOW).apply {
            itemMeta = itemMeta.also {
                it.isUnbreakable = true
            }
        },
        ItemStack(Material.ARROW, 2)
    )

    override val abilityName: String = "Sniper"
    override val abilityDescription: String =
        "Enchants your bow with power ${MiscUtils.intToRoman(powerLevel)} and punch ${MiscUtils.intToRoman(punchLevel)}! Can only be fired once."
    override val killPowerName: String = "Robin Hood"
    override val killPowerDescription: String = "Gives swiftness for ${swiftnessTicks.seconds}s"

    companion object {
        @field:Configurable("games.crumble.kits.archer.power_level")
        var powerLevel: Int = 3

        @field:Configurable("games.crumble.kits.archer.punch_level")
        var punchLevel: Int = 2

        @field:Configurable("games.crumble.kits.archer.swiftness_ticks")
        var swiftnessTicks: Long = 30
    }

    override fun onKill(killed: Player) {
        require(player != null) { "Cannot invoke methods on the kit template" }
        // TODO
    }

    override fun onAbility() {
        require(player != null) { "Cannot invoke methods on the kit template" }
        // TODO
    }

    override fun reset() {

    }
}