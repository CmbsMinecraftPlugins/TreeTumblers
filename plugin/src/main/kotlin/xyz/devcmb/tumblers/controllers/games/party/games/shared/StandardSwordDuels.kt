package xyz.devcmb.tumblers.controllers.games.party.games.shared

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.controllers.games.party.PartyController
import xyz.devcmb.tumblers.controllers.games.party.PartyGame
import xyz.devcmb.tumblers.item.Kit
import java.util.UUID

class StandardSwordDuels(
    party: PartyController?,
    currentMatchup: PartyController.PartyMatchup
) : PartyGame(party, currentMatchup) {
    // for templates
    constructor() : this(
        null,
        PartyController.PartyMatchup.IndividualMatchup(null, null, null)
    )

    override val id: String = "shared_standard_sword_duels"
    override val kit: Kit.KitDefinition = object : Kit.KitDefinition {
        override val items: ArrayList<Kit.KitItem> = arrayListOf(
            Kit.KitItem.StandardItem(ItemStack.of(Material.STONE_SWORD)),
            Kit.KitItem.ArmorItem(ItemStack(Material.LEATHER_BOOTS))
        )
        override val uuid: UUID = UUID.randomUUID()
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