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
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleKit
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.item.Kit
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.intToRoman
import java.util.UUID

class FisherKit(
    override val companion: CrumbleKit.Companion,
    override val player: TumblingPlayer,
    override val crumble: CrumbleController
) : CrumbleKit {
    companion object : CrumbleKit.Companion {
        val tridentLoyaltyLevel: Int = configurable("games.crumble.kits.fisher.trident_loyalty_level")
        val tridentDamage: Double = configurable("games.crumble.kits.fisher.trident_damage")
        val tridentAttackSpeed: Double = configurable("games.crumble.kits.fisher.trident_attack_speed")

        override val id: String = "fisher"
        override val name: String = "Fisher"
        override val kit: Kit.KitDefinition = object : Kit.KitDefinition {
            override val items: ArrayList<Kit.KitItem> = arrayListOf(
                Kit.KitItem.StandardItem(ItemStack.of(Material.TRIDENT).apply {
                    addEnchantment(Enchantment.LOYALTY, tridentLoyaltyLevel)
                    editMeta { meta ->
                        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE)
                        meta.addAttributeModifier(
                            Attribute.ATTACK_DAMAGE,
                            AttributeModifier(
                                NamespacedKey(TreeTumblers.NAMESPACE, "attack_damage_nerf"),
                                tridentDamage - 1,  // remove hand damage
                                AttributeModifier.Operation.ADD_NUMBER,
                            )
                        )
                        meta.removeAttributeModifier(Attribute.ATTACK_SPEED)
                        meta.addAttributeModifier(
                            Attribute.ATTACK_SPEED, AttributeModifier(
                                NamespacedKey(TreeTumblers.NAMESPACE, "attack_speed_nerf"),
                                tridentAttackSpeed - 4.0,  // hand default is 4
                                AttributeModifier.Operation.ADD_NUMBER,
                                EquipmentSlotGroup.HAND
                            )
                        )
                    }
                }),
                Kit.KitItem.StandardItem(ItemStack.of(Material.COD).apply {
                    editMeta { meta ->
                        meta.itemName(Component.text("Knockback Fish"))
                    }

                    addUnsafeEnchantment(Enchantment.KNOCKBACK, 1)
                }),
                Kit.KitItem.StandardItem(ItemStack(Material.STONE_PICKAXE)),
                Kit.KitItem.ArmorItem(ItemStack(Material.LEATHER_HELMET)),
                Kit.KitItem.ArmorItem(ItemStack(Material.LEATHER_BOOTS)),
                Kit.KitItem.ArmorItem(ItemStack(Material.LEATHER_LEGGINGS))
            )

            override val uuid: UUID = UUID.randomUUID()
        }

        override val abilityName: String = "Wrath of Clownfish"
        override val abilityDescription: String = "Gives your trident the power of zeus, striking anyone it hits with a bolt of lightning"
        override val killPowerName: String = "Fishy Fish"
        override val killPowerDescription: String = "Increases your knockback fish's knockback level by 1"

        override fun create(
            player: TumblingPlayer,
            crumble: CrumbleController
        ): CrumbleKit = FisherKit(this, player, crumble)
    }

    var knockbackLevel: Int = 1
    override fun onKill(killed: Player) {
        val fish = player.bukkitPlayer!!.inventory.first { it.type == Material.COD }
        if(fish == null) {
            DebugUtil.severe("Player ${player.name} lacks a knockback fish!")
            return
        }

        knockbackLevel += 1
        fish.removeEnchantments()
        fish.addUnsafeEnchantment(Enchantment.KNOCKBACK, knockbackLevel)
        player.bukkitPlayer!!.sendMessage(Format.success("Knockback fish upgraded to knockback level ${intToRoman(knockbackLevel)}!"))
    }

    var abilityActive: Boolean = false
    override fun onAbility() {
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
            || player != this.player.bukkitPlayer
            || !abilityActive
        ) return

        val strike = trident.world.strikeLightning(trident.location)
        strike.causingPlayer = player
        abilityActive = false
    }

    @EventHandler
    fun onItemConsume(event: PlayerItemConsumeEvent) {
        val item = event.item
        if (item.type != Material.COD) return
        event.isCancelled = true
    }
}