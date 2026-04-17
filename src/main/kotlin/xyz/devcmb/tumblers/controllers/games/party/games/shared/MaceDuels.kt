package xyz.devcmb.tumblers.controllers.games.party.games.shared

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.controllers.games.party.PartyController
import xyz.devcmb.tumblers.controllers.games.party.PartyGame
import xyz.devcmb.tumblers.util.Kit
import xyz.devcmb.tumblers.util.runTask

class MaceDuels(
    party: PartyController?,
    currentMatchup: PartyController.PartyMatchup
) : PartyGame(party, currentMatchup) {
    // for templates
    constructor() : this(
        null,
        PartyController.PartyMatchup.IndividualMatchup(null, null, null)
    )

    override val id: String = "shared_mace_duels"
    override val kit: Kit.KitDefinition = object : Kit.KitDefinition {
        override val items: ArrayList<ItemStack> = arrayListOf(ItemStack.of(Material.MACE).apply {
            itemMeta = itemMeta.also {
                it.addEnchant(Enchantment.DENSITY, 3, true)
                it.addEnchant(Enchantment.WIND_BURST, 1, true)
            }
        }, ItemStack.of(Material.WIND_CHARGE, 64))
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

    // https://github.com/666pyke/NoWindCharge/blob/main/src/main/java/org/me/pyke/nowindcharge/WindChargeListener.java
    @EventHandler
    fun playerWindChargeEvent(event: PlayerInteractEvent) {
        val player = event.player

        val mainHandItem: ItemStack = player.inventory.itemInMainHand
        val offHandItem: ItemStack = player.inventory.itemInOffHand

        if ((mainHandItem.type != Material.WIND_CHARGE && offHandItem.type != Material.WIND_CHARGE) ||
            (event.action !== Action.RIGHT_CLICK_AIR && event.action !== Action.RIGHT_CLICK_BLOCK)
        ) return


        if(player !in matchup.players) return

        runTask {
            val main = player.inventory.itemInMainHand
            val off = player.inventory.itemInOffHand

            if (main.type == Material.WIND_CHARGE) {
                main.amount = 64
            }

            if (off.type == Material.WIND_CHARGE) {
                off.amount = 64
            }
        }
    }
}