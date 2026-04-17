package xyz.devcmb.tumblers.controllers.games.party.games.shared

import io.papermc.paper.event.entity.EntityMoveEvent
import org.bukkit.Material
import org.bukkit.entity.Horse
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.controllers.games.party.PartyController
import xyz.devcmb.tumblers.controllers.games.party.PartyGame
import xyz.devcmb.tumblers.util.Kit

class SpearDuels(
    party: PartyController?,
    currentMatchup: PartyController.PartyMatchup
) : PartyGame(party, currentMatchup) {
    // for templates
    constructor() : this(
        null,
        PartyController.PartyMatchup.IndividualMatchup(null, null, null)
    )

    override val id: String = "shared_spear_duels"
    override val kit: Kit.KitDefinition = object : Kit.KitDefinition {
        override val items: ArrayList<ItemStack> = arrayListOf(ItemStack.of(Material.NETHERITE_SPEAR))
        override val teamArmorSlot: EquipmentSlot? = EquipmentSlot.FEET
    }

    override val team: Boolean = true
    override val individual: Boolean = true

    val horses: HashMap<Player, Horse> = HashMap()

    override fun postSpawn() {
        matchup.players.forEach {
            partyController!!.map.world.spawn(it.location, Horse::class.java) { entity ->
                entity.inventory.saddle = ItemStack(Material.SADDLE)
                entity.isInvulnerable = true
                entity.addPassenger(it)
                horses.put(it, entity)
            }
        }
    }

    override suspend fun start() {
    }

    override fun cleanup() {
        horses.forEach { it.value.remove() }
    }

    @EventHandler
    fun playerDismountEvent(event: VehicleExitEvent) {
        val player = event.exited
        if(player !is Player || player !in matchup.players) return

        event.isCancelled = true
    }

    @EventHandler
    fun horseMoveEvent(event: EntityMoveEvent) {
        if(event.entity !is Horse || event.entity !in horses.values) return

        val player = event.entity.passengers.first { it is Player }
        if(player == null || !partyController!!.frozenPlayers.contains(player)) return

        event.isCancelled = true
    }

    @EventHandler
    fun playerDeathEvent(event: PlayerDeathEvent) {
        val player = event.player
        if(player !in matchup.players) return

        if(!horses.contains(player)) return

        horses[player]!!.remove()
        horses.remove(player)
    }
}