package xyz.devcmb.tumblers.controllers.games.party.games.individual

import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.controllers.games.party.IndividualPartyGame
import xyz.devcmb.tumblers.util.Kit

class StandardSwordDuels(
    val player1: Player?,
    val player2: Player?
) : IndividualPartyGame {
    // for templates
    constructor() : this(null, null)

    override val id: String = "indiv_standard_sword_duels"
    override val kit: Kit.KitDefinition = object : Kit.KitDefinition {
        override val items: ArrayList<ItemStack> = arrayListOf()
        override val teamArmorSlot: EquipmentSlot? = EquipmentSlot.FEET
    }

    override fun postSpawn() {
        require(player1 != null || player2 != null) { "Templates cannot invoke post spawn" }
    }

    override suspend fun start() {
        require(player1 != null || player2 != null) { "Templates cannot start" }
    }

    override fun cleanup() {
        require(player1 != null || player2 != null) { "Templates cannot cleanup" }
    }
}