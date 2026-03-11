package xyz.devcmb.tumblers.controllers.games.crumble

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

interface Kit : Listener {
    val player: Player?
    val id: String
    val name: String
    val inventoryModel: NamespacedKey
    val items: ArrayList<ItemStack>

    val abilityName: String
    val abilityDescription: String

    val killPowerName: String
    val killPowerDescription: String

    val kitIcon: String
    val kitDisplayTextLength: Int

    fun onKill(killed: Player)
    fun onAbility()

    fun reset()
}