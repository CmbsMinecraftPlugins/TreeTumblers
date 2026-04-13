package xyz.devcmb.tumblers.controllers.games.party.games.shared

import org.bukkit.Material
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.controllers.games.party.PartyController
import xyz.devcmb.tumblers.controllers.games.party.PartyGame
import xyz.devcmb.tumblers.util.Kit

class StandardSwordDuels(
    val currentType: PartyController.PartyGameType,
    val matchup: PartyController.PartyMatchup
) : PartyGame {
    // for templates
    constructor() : this(
        PartyController.PartyGameType.INDIVIDUAL,
        PartyController.PartyMatchup.IndividualMatchup(null, null, null)
    )

    override val id: String = "shared_standard_sword_duels"
    override val kit: Kit.KitDefinition = object : Kit.KitDefinition {
        override val items: ArrayList<ItemStack> = arrayListOf(ItemStack.of(Material.STONE_SWORD))
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