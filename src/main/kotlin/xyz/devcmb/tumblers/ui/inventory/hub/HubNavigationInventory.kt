package xyz.devcmb.tumblers.ui.inventory.hub

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.devcmb.invcontrol.chest.ChestInventoryPage
import xyz.devcmb.invcontrol.chest.ChestInventoryUI
import xyz.devcmb.invcontrol.chest.InventoryItem
import xyz.devcmb.tumblers.TumblingGenericException
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.buttonClickSound
import xyz.devcmb.tumblers.util.fadeTp
import xyz.devcmb.tumblers.util.validateLocation

class HubNavigationInventory(
    val player: Player,
    override val id: String = "hubNavigationInventory",
) : HandledInventory {
    companion object {
        @field:Configurable("lobby.spawn.navigate")
        var navigationPosition: List<Int> = listOf(-73, 202, 8, -90, 0)

        @field:Configurable("lobby.world")
        var lobbyWorld: String = "hub"
    }

    override val inventory: ChestInventoryUI = ChestInventoryUI(
        player,
        Format.mm("Hub Navigator"),
        1
    ).apply {
        val page = ChestInventoryPage()
        addPage("main", page, true)

        page.addItem(InventoryItem(
            getItemStack = { page, item ->
                ItemStack.of(Material.OAK_LOG).apply {
                    itemMeta = itemMeta.also {
                        it.itemName(Format.mm("<yellow>Lodge</yellow>"))
                    }
                }
            },
            slot = 0,
            onClick = { page, item ->
                page.ui.close()
                player.buttonClickSound()

                val location = navigationPosition.validateLocation(Bukkit.getWorld(lobbyWorld)!!)
                    ?: throw TumblingGenericException("Hub navigation position for lodge building is not a valid position!")

                player.fadeTp(location)
            }
        ))
    }
}