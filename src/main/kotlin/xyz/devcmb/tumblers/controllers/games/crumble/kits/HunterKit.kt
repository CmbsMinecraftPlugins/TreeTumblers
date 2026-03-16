package xyz.devcmb.tumblers.controllers.games.crumble.kits

import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.CrossbowMeta
import org.bukkit.inventory.meta.FireworkMeta
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.controllers.games.crumble.Kit

class HunterKit(
    override val player: Player?,
    override val crumble: CrumbleController,
) : Kit {
    override val id: String = "hunter"
    override val name: String = "Hunter"
    override val inventoryModel: NamespacedKey = NamespacedKey("tumbling", "crumble/hunter")
    override val items: ArrayList<ItemStack> = arrayListOf(
        ItemStack(Material.WOODEN_SWORD).apply {
            addEnchantment(Enchantment.KNOCKBACK, 1)
        },
        ItemStack(Material.CROSSBOW),
        ItemStack(Material.ARROW, 4),
        ItemStack(Material.LEATHER_HELMET),
    )

    override val abilityName: String = "Multishot"
    override val abilityDescription: String = "Split your bow in three with multishot on your bow for one shot"
    override val killPowerName: String = "Blast Off"
    override val killPowerDescription: String = "Gives you a firework to charge your crossbow with"

    override val kitIcon: String = "\uE003"
    override val kitDisplayTextLength: Int = 47
    override fun onKill(killed: Player) {
        require(player != null) { "Cannot invoke methods on the kit template" }

        player.inventory.addItem(ItemStack(Material.FIREWORK_ROCKET).apply {
            itemMeta = (itemMeta as FireworkMeta).also { meta ->
                meta.addEffect(
                    FireworkEffect.builder()
                        .withColor(Color.RED, Color.ORANGE, Color.YELLOW)
                        .withTrail()
                        .withFlicker()
                        .build()
                )
            }
        })
    }

    var abilityActive = false
    override fun onAbility() {
        require(player != null) { "Cannot invoke methods on the kit template" }
        val bow = player.inventory.first { it.type == Material.CROSSBOW }!!
        bow.addEnchantment(Enchantment.MULTISHOT, 1)
        bow.itemMeta = (bow.itemMeta as CrossbowMeta).apply {
            if(hasChargedProjectiles()) {
                setChargedProjectiles(listOf(
                    chargedProjectiles[0].clone(),
                    chargedProjectiles[0].clone(),
                    chargedProjectiles[0].clone()
                ))
            }
        }

        abilityActive = true
    }

    @EventHandler
    fun shootEvent(event: ProjectileLaunchEvent) {
        val entity = event.entity
        if(entity !is Arrow) return

        val shooter = entity.shooter
        if(
            shooter !is Player
            || player != shooter
            || !abilityActive
        ) return

        val bow = player.inventory.first { it.type == Material.CROSSBOW } ?: return
        bow.removeEnchantments()
        abilityActive = false
    }
}