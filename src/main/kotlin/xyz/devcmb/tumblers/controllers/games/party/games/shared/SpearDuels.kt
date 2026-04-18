package xyz.devcmb.tumblers.controllers.games.party.games.shared

import org.bukkit.Material
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
        override val items: ArrayList<ItemStack> = arrayListOf(ItemStack.of(Material.DIAMOND_SPEAR))
        override val teamArmorSlot: EquipmentSlot? = EquipmentSlot.FEET
    }

    override val team: Boolean = true
    override val individual: Boolean = true

    override fun postSpawn() {
    }

    override suspend fun start() {
    }

    override fun cleanup() {
    }
}