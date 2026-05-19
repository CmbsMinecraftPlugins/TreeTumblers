package xyz.devcmb.tumblers.ui.inventory.hub

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.devcmb.invcontrol.chest.ChestInventoryPage
import xyz.devcmb.invcontrol.chest.ChestInventoryUI
import xyz.devcmb.invcontrol.chest.InventoryItem
import xyz.devcmb.tumblers.TumblingGenericException
import xyz.devcmb.tumblers.controllers.server.WorldController
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.buttonClickSound
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.fadeTp
import xyz.devcmb.tumblers.util.toCenterXZLocation
import xyz.devcmb.tumblers.util.validateLocation

class HubNavigationInventory(
    val player: Player,
    override val id: String = "hubNavigationInventory",
) : HandledInventory {
    val lodgeNavigationPosition: List<Int> = configurable("lobby.navigator.lodge")
    val practiceCoursesNavigationPosition: List<Int> = configurable("lobby.navigator.practice_courses")

    override val inventory: ChestInventoryUI = ChestInventoryUI(
        player,
        Format.mm("<white>Hub Navigator</white>"),
        1
    ).apply {
        val page = ChestInventoryPage()
        addPage("main", page, true)

        page.addItem(HubNavigationEntry(
            ItemStack.of(Material.OAK_LOG).apply {
                editMeta {
                    it.itemName(Format.mm("<yellow>Lodge</yellow>"))
                }
            },
            0,
            lodgeNavigationPosition.validateLocation(Bukkit.getWorld(WorldController.lobbyWorld)!!)
                    ?: throw TumblingGenericException("Hub navigation position for lodge building is not a valid position!")
        ))

        page.addItem(HubNavigationEntry(
            ItemStack.of(Material.DARK_OAK_LOG).apply {
                editMeta {
                    it.itemName(Format.mm("<aqua>Practice Courses</aqua>"))
                }
            },
            1,
            practiceCoursesNavigationPosition.validateLocation(Bukkit.getWorld(WorldController.lobbyWorld)!!)
                    ?: throw TumblingGenericException("Hub navigation position for practice courses is not a valid position!")
        ))
    }

    class HubNavigationEntry(
        val itemStack: ItemStack,
        itemSlot: Int,
        val location: Location
    ) : InventoryItem(
        getItemStack = { _,_ ->
            itemStack
        },
        slot = itemSlot,
        onClick = { page, item ->
            page.ui.close()
            val player = page.ui.player
            player.buttonClickSound()
            player.fadeTp(location.toCenterXZLocation())
        }
    )
}