package xyz.devcmb.tumblers.ui.inventory.hub

import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.interfaces.ChestInterfaceBuilder
import com.noxcrew.interfaces.interfaces.buildChestInterface
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.TumblingException
import xyz.devcmb.tumblers.controllers.server.WorldController
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.buttonClickSound
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.fadeTp
import xyz.devcmb.tumblers.util.toCenterXZLocation
import xyz.devcmb.tumblers.util.validateLocation

class HubNavigationInventory : HandledInventory {
    override val id: String = "hubNavigationInventory"

    val lodgeNavigationPosition: List<Int> = configurable("lobby.navigator.lodge")
    val practiceCoursesNavigationPosition: List<Int> = configurable("lobby.navigator.practice_courses")

    override val inventory = buildChestInterface {
        titleSupplier = { Component.text("Hub Navigator", NamedTextColor.WHITE) }
        rows = 1

        hubNavigationEntry(GridPoint(0,0), ItemStack.of(Material.OAK_LOG).apply {
            editMeta {
                it.itemName(Format.mm("<yellow>Lodge</yellow>"))
            }
        }, lodgeNavigationPosition.validateLocation(Bukkit.getWorld(WorldController.lobbyWorld)!!)
            ?: throw TumblingException("Hub navigation position for lodge building is not a valid position!"))

        hubNavigationEntry(GridPoint(0,1), ItemStack.of(Material.DARK_OAK_LOG).apply {
            editMeta {
                it.itemName(Format.mm("<aqua>Practice Courses</aqua>"))
            }
        }, practiceCoursesNavigationPosition.validateLocation(Bukkit.getWorld(WorldController.lobbyWorld)!!)
            ?: throw TumblingException("Hub navigation position for practice courses is not a valid position!"))
    }

    private fun ChestInterfaceBuilder.hubNavigationEntry(point: GridPoint, stack: ItemStack, location: Location) {
        withTransform { pane, view ->
            pane[point] = StaticElement(drawable(stack)) {
                val player = it.player
                player.buttonClickSound()
                player.fadeTp(location.toCenterXZLocation())
                it.view.close(TreeTumblers.pluginScope)
            }
        }
    }
}