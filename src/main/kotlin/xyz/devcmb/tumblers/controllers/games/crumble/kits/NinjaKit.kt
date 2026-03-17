package xyz.devcmb.tumblers.controllers.games.crumble.kits

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.libraryaddict.disguise.DisguiseAPI
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.entity.Fireball
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.controllers.games.crumble.Kit
import xyz.devcmb.tumblers.util.runTaskLater
import xyz.devcmb.tumblers.util.tickSeconds
import xyz.devcmb.tumblers.util.tumblingPlayer
import kotlin.random.Random

class NinjaKit(
    override val player: Player?,
    override val crumble: CrumbleController
) : Kit {
    override val id: String = "ninja"
    override val name: String = "Ninja"
    override val inventoryModel: NamespacedKey = NamespacedKey("tumbling", "crumble/ninja")
    override val items: ArrayList<ItemStack> = arrayListOf(
        ItemStack(Material.STONE_SWORD),
        ItemStack(Material.LEATHER_BOOTS)
    )

    override val abilityName: String = "Stealth"
    override val abilityDescription: String = "Become the stealth master you've always wanted to be. Turns you invisible for ${invisibilityDuration.toLong().tickSeconds}s and creates a dummy in your place."
    override val killPowerName: String = "Vanish"
    override val killPowerDescription: String = "Gives you a smoke bomb"

    override val kitIcon: String = "\uE004"
    override val kitDisplayTextLength: Double = 38.5

    companion object {
        val ninjaOwnerKey = NamespacedKey("tumbling", "ninja_owner")
        val ninjaSmokeBombKey = NamespacedKey("tumbling", "ninja_smoke_bomb")

        @field:Configurable("games.crumble.kits.ninja.invisibility_duration")
        var invisibilityDuration: Int = 80

        @field:Configurable("games.crumble.kits.ninja.smoke_duration")
        var smokeDuration: Int = 100

        @field:Configurable("games.crumble.kits.ninja.smoke_size")
        var smokeSize: Double = 2.5
    }

    override fun onKill(killed: Player) {
        require(player != null) { "Cannot invoke methods on the kit template" }
        player.inventory.addItem(ItemStack.of(Material.FIRE_CHARGE).apply {
            itemMeta = itemMeta.also {
                it.itemName(Component.text("Smoke Bomb", NamedTextColor.GOLD))
                it.persistentDataContainer.set(
                    ninjaSmokeBombKey,
                    PersistentDataType.BOOLEAN,
                    true
                )
            }
        })
    }

    var abilityZombie: Zombie? = null
    override fun onAbility() {
        require(player != null) { "Cannot invoke methods on the kit template" }

        DisguiseAPI.disguiseNextEntity(PlayerDisguise(player).setNameVisible(false))

        val zombie = player.world.spawn(player.location, Zombie::class.java)
        zombie.setCanBreakDoors(false)
        zombie.setShouldBurnInDay(false)
        zombie.addPotionEffect(PotionEffect(PotionEffectType.SPEED, -1, 2, false, false, false))

        val dataContainer = zombie.persistentDataContainer
        dataContainer.set(
            ninjaOwnerKey,
            PersistentDataType.STRING,
            player.uniqueId.toString()
        )

        val equipment = zombie.equipment
        equipment.bootsDropChance = 0f
        equipment.leggingsDropChance = 0f
        equipment.chestplateDropChance = 0f
        equipment.helmetDropChance = 0f
        equipment.itemInMainHandDropChance = 0f

        equipment.setItemInMainHand(ItemStack(Material.WOODEN_SWORD))
        equipment.helmet = ItemStack(Material.LEATHER_HELMET).apply {
            itemMeta = (itemMeta as LeatherArmorMeta).also {
                it.setColor(Color.fromRGB(player.tumblingPlayer!!.team.color.value()))
            }
        }
        abilityZombie = zombie

        player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, invisibilityDuration, 0))
        runTaskLater(invisibilityDuration.toLong()) {
            abilityZombie?.remove()
        }
    }

    override fun cleanup() {
        abilityZombie?.remove()
        abilityZombie = null
    }

    @EventHandler
    fun onZombieTarget(event: EntityTargetLivingEntityEvent) {
        val target = event.target
        if(target !is Player) return

        val entity = event.entity
        if(entity.uniqueId != abilityZombie?.uniqueId) return

        val targetTeam = target.tumblingPlayer?.team ?: return
        if(targetTeam == player!!.tumblingPlayer?.team) event.isCancelled = true
    }

    @EventHandler
    fun onSmokeBombThrow(event: PlayerInteractEvent) {
        val player = event.player
        if(player !== this.player) return

        val item = event.item ?: return
        val container = item.persistentDataContainer
        if(container.get(ninjaSmokeBombKey, PersistentDataType.BOOLEAN) == true) {
            item.amount--

            // TODO: This is just for development
            // Replace this with a curved, velocity-based, gravity-affected projectile instead of a straight fireball
            // Maybe @nibbl-z can do this :thinking:
            val fireball = player.world.spawn(player.location, Fireball::class.java)
            fireball.persistentDataContainer.set(ninjaSmokeBombKey, PersistentDataType.BOOLEAN, true)
        }
    }

    var smokeBombs: ArrayList<Location> = ArrayList()
    @EventHandler
    fun smokeBombDetonateEvent(event: EntityExplodeEvent) {
        // TODO: Remove and replace with a different handler
        val entity = event.entity
        if(
            entity !is Fireball
            || entity.persistentDataContainer.get(ninjaSmokeBombKey, PersistentDataType.BOOLEAN) != true
        ) return

        event.isCancelled = true
        smokeBombs.add(event.location)
        runTaskLater(smokeDuration.toLong()) {
            smokeBombs.remove(event.location)
        }
    }

    @EventHandler
    fun tickEvent(event: ServerTickStartEvent) {
        smokeBombs.forEach { loc ->
            repeat(5) {
                crumble.currentMap.world.spawnParticle(
                    Particle.CAMPFIRE_SIGNAL_SMOKE,
                    loc.clone().add(
                        Random.nextDouble(-smokeSize, smokeSize),
                        Random.nextDouble(-smokeSize, smokeSize),
                        Random.nextDouble(-smokeSize, smokeSize)
                    ),
                    20,
                    0.0,
                    0.0,
                    0.0,
                    0.0
                )
            }
        }
    }
}