package xyz.devcmb.tumblers.ui.inventory.hub

import com.noxcrew.noxesium.core.registry.CommonItemComponentTypes
import com.noxcrew.noxesium.paper.component.setNoxesiumComponent
import org.bukkit.Bukkit
import org.bukkit.Location
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
import xyz.devcmb.tumblers.util.toCenterXZLocation
import xyz.devcmb.tumblers.util.validateLocation

class HubNavigationInventory(
    val player: Player,
    override val id: String = "hubNavigationInventory",
) : HandledInventory {
    companion object {
        @field:Configurable("lobby.navigator.lodge")
        var lodgeNavigationPosition: List<Int> = listOf(-73, 202, 8, -90, 0)

        @field:Configurable("lobby.navigator.practice_courses")
        var practiceCoursesNavigationPosition: List<Int> = listOf(40, 201, -92, 180, 0)

        @field:Configurable("lobby.world")
        var lobbyWorld: String = "hub"
    }

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
            lodgeNavigationPosition.validateLocation(Bukkit.getWorld(lobbyWorld)!!)
                    ?: throw TumblingGenericException("Hub navigation position for lodge building is not a valid position!")
        ))

        page.addItem(HubNavigationEntry(
            ItemStack.of(Material.DARK_OAK_LOG).apply {
                editMeta {
                    it.itemName(Format.mm("<aqua>Practice Courses</aqua>"))
                }
            },
            1,
            practiceCoursesNavigationPosition.validateLocation(Bukkit.getWorld(lobbyWorld)!!)
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