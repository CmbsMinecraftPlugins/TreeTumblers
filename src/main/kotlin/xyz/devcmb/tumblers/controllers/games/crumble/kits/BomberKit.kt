package xyz.devcmb.tumblers.controllers.games.crumble.kits

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.controllers.games.crumble.Kit
import xyz.devcmb.tumblers.util.DebugUtil
import java.util.UUID

class BomberKit(
    override val player: Player?,
    override val crumble: CrumbleController,
) : Kit {
    override val id: String = "bomber"
    override val name: String = "Bomber"
    override val inventoryModel: NamespacedKey = NamespacedKey("tumbling", "crumble/bomber")
    override val items: ArrayList<ItemStack> = arrayListOf(
        ItemStack(Material.WOODEN_SWORD),
        ItemStack(Material.TNT, 2),
        ItemStack(Material.LEATHER_HELMET).apply {
            addUnsafeEnchantment(Enchantment.BLAST_PROTECTION, blastProtectionLevel)
        },
        ItemStack(Material.LEATHER_CHESTPLATE).apply {
            addUnsafeEnchantment(Enchantment.BLAST_PROTECTION, blastProtectionLevel)
        },
        ItemStack(Material.LEATHER_LEGGINGS).apply {
            addUnsafeEnchantment(Enchantment.BLAST_PROTECTION, blastProtectionLevel)
        },
        ItemStack(Material.LEATHER_BOOTS).apply {
            addUnsafeEnchantment(Enchantment.BLAST_PROTECTION, blastProtectionLevel)
        }
    )

    override val abilityName: String = "Nuke"
    override val abilityDescription: String = "Gives a large-radius explosive that deals ${(nukeDamage / 2.0)} hearts and explodes instantly!"
    override val killPowerName: String = "Aw Man"
    override val killPowerDescription: String = "Gives you a creeper spawn egg"

    override val kitIcon: String = "\uE001"
    override val kitDisplayTextLength: Int = 49

    companion object {
        val nukeKey = NamespacedKey("tumbling", "nuke")
        val nukeIdKey = NamespacedKey("tumbling", "nuke_id")

        @field:Configurable("games.crumble.kits.bomber.nuke_explosion_ticks")
        var nukeExplosionTicks = 5

        @field:Configurable("games.crumble.kits.bomber.nuke_radius")
        var nukePower: Float = 7.0f

        @field:Configurable("games.crumble.kits.bomber.nuke_damage")
        var nukeDamage: Int = 7

        @field:Configurable("games.crumble.kits.bomber.blast_protection_level")
        var blastProtectionLevel: Int = 2
    }

    override fun onKill(killed: Player) {
        require(player != null) { "Cannot invoke methods on the kit template" }
        player.inventory.addItem(ItemStack(Material.CREEPER_SPAWN_EGG))
    }

    var nukeId: UUID? = null
    override fun onAbility() {
        require(player != null) { "Cannot invoke methods on the kit template" }
        val item = ItemStack.of(Material.TNT).apply {
            itemMeta = itemMeta.also {
                it.itemName(Component.text("Nuke", NamedTextColor.RED))
                it.setEnchantmentGlintOverride(true)
                it.persistentDataContainer.set(
                    nukeKey,
                    PersistentDataType.BOOLEAN,
                    true
                )

                val id = UUID.randomUUID()
                nukeId = id
                it.persistentDataContainer.set(
                    nukeIdKey,
                    PersistentDataType.STRING,
                    id.toString()
                )
            }
        }
        crumble.kitItems.add(item)
        player.inventory.addItem(item)
    }

    @EventHandler
    fun onTntExplode(event: ExplosionPrimeEvent) {
        val entity = event.entity
        if(entity !is TNTPrimed) return

        val dataContainer = entity.persistentDataContainer
        val id = dataContainer.get(nukeIdKey, PersistentDataType.STRING)
        if(id != nukeId.toString()) return

        if (dataContainer.get(nukeKey, PersistentDataType.BOOLEAN) == true) {
            event.isCancelled = true
            entity.world.createExplosion(
                entity,
                entity.location,
                nukePower,
                false,
                true
            )

            entity.remove()
        }
    }

    val hitPlayers: HashMap<UUID, ArrayList<Player>> = hashMapOf()
    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityDamage(event: EntityDamageEvent) {
        if(event.isCancelled) return

        val player = event.entity
        if(player !is Player) return

        val causingEntity = event.damageSource.directEntity
        if(causingEntity == null) return

        val dataContainer = causingEntity.persistentDataContainer
        if (dataContainer.get(nukeKey, PersistentDataType.BOOLEAN) == true) {
            DebugUtil.info("Nuke found, setting damage for ${player.name} to $nukeDamage")
            event.damage = nukeDamage.toDouble()
            hitPlayers.putIfAbsent(causingEntity.uniqueId, arrayListOf())
            hitPlayers[causingEntity.uniqueId]!!.add(player)
        }
    }

    override fun cleanup() {
        hitPlayers.clear()
        nukeId = null
    }
}