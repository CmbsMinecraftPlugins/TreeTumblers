package xyz.devcmb.tumblers.controllers.games.crumble.kits

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleBadge
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.controllers.games.crumble.Kit
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.runTaskLater
import xyz.devcmb.tumblers.util.tickSeconds
import xyz.devcmb.tumblers.util.tumblingPlayer

class WorkerKit(
    override val player: TumblingPlayer?,
    override val crumble: CrumbleController
) : Kit {
    val megaMineDuration: Long = configurable("games.crumble.kits.worker.megamine_duration")
    val efficiencyDuration: Long = configurable("games.crumble.kits.worker.efficiency_duration")

    override val id: String = "worker"
    override val name: String = "Worker"
    override val inventoryModel: NamespacedKey = NamespacedKey(TreeTumblers.NAMESPACE, "crumble/worker")
    override val items: ArrayList<ItemStack> = arrayListOf(
        ItemStack(Material.STONE_SWORD),
        ItemStack(Material.WOODEN_AXE),
        ItemStack(Material.IRON_PICKAXE),
        ItemStack(Material.IRON_SHOVEL),
        ItemStack(Material.LEATHER_BOOTS)
    )

    override val abilityName: String = "Megamine"
    override val abilityDescription: String = "You yearn for the mines' TNT but just can't get it. Lets your pickaxe mine a 3x3x3 volume for ${megaMineDuration.tickSeconds}s"
    override val killPowerName: String = "Efficiency"
    override val killPowerDescription: String = "Gives you efficiency II on your tools for ${efficiencyDuration.tickSeconds}s"

    override val kitIcon: String = "\uE007"
    override val kitDisplayTextLength: Double = 48.5

    var kills: Int = 0
    override fun onKill(killed: Player) {
        require(player != null) { "Cannot invoke methods on the kit template" }

        val stacks: ArrayList<ItemStack> = ArrayList()
        killed.inventory.forEach {
            try {
                it.addEnchantment(Enchantment.EFFICIENCY, 2)
                stacks.add(it)
            } catch(e: Exception) {}
        }

        kills++

        runTaskLater(efficiencyDuration) {
            kills--
            if(kills <= 0) {
                kills = 0
                stacks.forEach {
                    it.removeEnchantment(Enchantment.EFFICIENCY)
                }
            }
        }
    }

    var abilityActive = false
    override fun onAbility() {
        require(player != null) { "Cannot invoke methods on the kit template" }
        require(player.isOnline) { "Player must be online to invoke methods on the kit" }

        val pick = player.bukkitPlayer!!.inventory.first { it.type == items[2].type }
        pick.itemMeta = pick.itemMeta.also {
            it.setEnchantmentGlintOverride(true)
            it.lore(arrayListOf(
                Component.text("Megamine", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
            ))
        }

        abilityActive = true

        runTaskLater(megaMineDuration) {
            abilityActive = false
            pick.itemMeta = pick.itemMeta.also {
                it.setEnchantmentGlintOverride(null)
                it.lore(arrayListOf())
            }
        }
    }

    val processingBlocks = mutableSetOf<Block>()
    val brokenBlocks = mutableSetOf<Location>()

    @EventHandler
    fun playerMineEvent(event: BlockBreakEvent) {
        val player = event.player
        if(player != this.player) return

        val item = player.inventory.itemInMainHand
        val origin = event.block

        if (item.type != items[2].type || !abilityActive) return
        if (!processingBlocks.add(origin)) return

        val originLocation = origin.location
        brokenBlocks.add(originLocation)

        for (x in -1..1)
        for (y in -1..1)
        for (z in -1..1) {
            val location = originLocation.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
            val block = location.block

            if (!processingBlocks.add(block)) continue
            player.breakBlock(block)
            brokenBlocks.add(location)
        }

        processingBlocks.remove(origin)
    }

    val lastPlayerStandingBlocks: HashMap<Player, Location> = HashMap()

    @EventHandler
    fun playerMoveEvent(event: PlayerMoveEvent) {
        val player = event.player
        val blockBelow = player.location.block.getRelative(BlockFace.DOWN)
        if(blockBelow.isSolid) {
            lastPlayerStandingBlocks[player] = blockBelow.location
        }
    }

    @EventHandler
    fun playerDeathEvent(event: EntityDeathEvent) {
        val player = event.entity as? Player ?: return
        val lastPosition = lastPlayerStandingBlocks[player] ?: return

        if(lastPosition in brokenBlocks && player != this.player?.bukkitPlayer) {
            crumble.grantBadge(this.player!!, CrumbleBadge.BATTLE_WORKER)
        }
    }

    override fun cleanup() {
        abilityActive = false
        kills = 0
        lastPlayerStandingBlocks.clear()
        brokenBlocks.clear()
    }
}