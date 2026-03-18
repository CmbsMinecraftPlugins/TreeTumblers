package xyz.devcmb.tumblers.controllers.games.crumble.kits

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.entity.Trident
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.controllers.games.crumble.Kit
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import java.util.UUID

class FisherKit(
    override val player: Player?,
    override val crumble: CrumbleController
) : Kit {
    override val id: String = "fisher"
    override val name: String = "Fisher"
    override val inventoryModel: NamespacedKey = NamespacedKey("tumbling", "crumble/fisher")
    override val items: ArrayList<ItemStack> = arrayListOf(
        ItemStack.of(Material.TRIDENT).apply {
            addEnchantment(Enchantment.LOYALTY, tridentLoyaltyLevel)
            itemMeta = itemMeta.also { meta ->
                meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE)
                meta.addAttributeModifier(
                    Attribute.ATTACK_DAMAGE,
                    AttributeModifier(
                        NamespacedKey.fromString(UUID.randomUUID().toString())!!,
                        tridentDamage - 1,  // remove hand damage
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.HAND
                    )
                )
                meta.removeAttributeModifier(Attribute.ATTACK_SPEED)
                meta.addAttributeModifier(
                    Attribute.ATTACK_SPEED, AttributeModifier(
                        NamespacedKey.fromString(UUID.randomUUID().toString())!!,
                        tridentAttackSpeed - 4.0,  // hand default is 4
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.HAND
                    )
                )
            }
        },
        ItemStack.of(Material.COD).apply {
            itemMeta = itemMeta.also { meta ->
                meta.itemName(Component.text("Knockback Fish"))
            }

            addUnsafeEnchantment(Enchantment.KNOCKBACK, 1)
        },
        ItemStack(Material.STONE_PICKAXE),
        ItemStack(Material.LEATHER_HELMET),
        ItemStack(Material.LEATHER_BOOTS),
        ItemStack(Material.LEATHER_LEGGINGS)
    )

    override val abilityName: String = "Wrath of Clownfish"
    override val abilityDescription: String = "Gives your trident the power of zeus, striking anyone it hits with a bolt of lightning"
    override val killPowerName: String = "Fishy Fish"
    override val killPowerDescription: String = "Increases your knockback fish's knockback level by 1"

    override val kitIcon: String = "\uE002"
    override val kitDisplayTextLength: Double = 45.5

    companion object {
        @field:Configurable("games.crumble.kits.fisher.trident_loyalty_level")
        var tridentLoyaltyLevel: Int = 2

        @field:Configurable("games.crumble.kits.fisher.trident_damage")
        var tridentDamage: Double = 5.0

        @field:Configurable("games.crumble.kits.fisher.trident_attack_speed")
        var tridentAttackSpeed: Double = 1.1
    }

    var knockbackLevel: Int = 1
    override fun onKill(killed: Player) {
        require(player != null) { "Cannot invoke methods on the kit template" }

        val fish = killed.inventory.first { it.type == Material.COD }
        if(fish == null) {
            DebugUtil.severe("Player ${player.name} lacks a knockback fish!")
            return
        }

        knockbackLevel += 1
        fish.removeEnchantments()
        fish.addUnsafeEnchantment(Enchantment.KNOCKBACK, knockbackLevel)
        player.sendMessage(Format.success("Knockback fish upgraded to knockback level ${MiscUtils.intToRoman(knockbackLevel)}!"))
    }

    var abilityActive: Boolean = false
    override fun onAbility() {
        require(player != null) { "Cannot invoke methods on the kit template" }
        abilityActive = true
    }

    override fun cleanup() {
        knockbackLevel = 1
        abilityActive = false
    }

    @EventHandler
    fun projectileHitEvent(event: ProjectileHitEvent) {
        val trident = event.entity
        if (trident !is Trident) return

        val player = trident.shooter
        if (
            player !is Player
            || player != this.player
            || !abilityActive
        ) return

        val strike = trident.world.strikeLightning(trident.location)
        strike.causingPlayer = player
        abilityActive = false
    }
}