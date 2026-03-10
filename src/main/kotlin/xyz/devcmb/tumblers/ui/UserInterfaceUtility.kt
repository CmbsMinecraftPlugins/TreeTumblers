package xyz.devcmb.tumblers.ui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.PlayerController

object UserInterfaceUtility {
    val SPACES = NamespacedKey("tumbling", "spaces")
    val WARNINGS = NamespacedKey("tumbling", "warnings")

    fun constructLine(length: Int, color: NamedTextColor = NamedTextColor.WHITE): Component {
        var component = Component.empty()

       repeat(length) {
            component = component.append(
                Component.text("—", color)
                    .append(
                        Component.text("\uF000")
                            .font(SPACES)
                    )
            )
        }

        return component
    }

    fun refreshAll(id: String) {
        val playerController = ControllerDelegate.getController("playerController") as PlayerController
        playerController.playerUIControllers.forEach { player, controller ->
            val inv = controller.inventories.find { it.id == id }
            require(inv != null) { "Inventory with an id of $id was not found for ${player.name}" }
            inv.inventory.reload()
        }
    }
}