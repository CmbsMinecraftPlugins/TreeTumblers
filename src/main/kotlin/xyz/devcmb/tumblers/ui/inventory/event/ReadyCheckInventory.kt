package xyz.devcmb.tumblers.ui.inventory.event

import com.noxcrew.noxesium.api.util.Unit
import com.noxcrew.noxesium.core.registry.CommonItemComponentTypes
import com.noxcrew.noxesium.paper.component.setNoxesiumComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.devcmb.invcontrol.chest.ChestInventoryPage
import xyz.devcmb.invcontrol.chest.ChestInventoryUI
import xyz.devcmb.invcontrol.chest.InventoryItem
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.event.EventController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.ui.inventory.components.ConfirmationButton

class ReadyCheckInventory(
    val player: Player,
    val eventController: EventController,
    override val id: String = "readyCheckInventory",
) : HandledInventory {
    override val inventory: ChestInventoryUI = ChestInventoryUI(
        player,
        UserInterfaceUtility.negativeSpace(8)
            .append(Component.text("\uE000", NamedTextColor.WHITE).font(NamespacedKey(TreeTumblers.NAMESPACE, "containers")))
            .append(UserInterfaceUtility.negativeSpace(UserInterfaceUtility.FULL_INVENTORY_NEGATIVE_ADVANCE))
            .append(Component.text("Are you ready?", NamedTextColor.WHITE).font(NamespacedKey("minecraft", "default"))),
        1
    ).apply {
        val page = ChestInventoryPage()
        addPage("main", page, true)

        val yesConfirmation = ConfirmationButton(ConfirmationButton.ConfirmationButtonType.YES, 2) { page, item ->
            page.ui.close()
            eventController.markReady(player)
        }

        val noConfirmation = ConfirmationButton(ConfirmationButton.ConfirmationButtonType.NO, 6) { page, item ->
            page.ui.close()
            eventController.markNotReady(player)
        }

        (yesConfirmation.items() + noConfirmation.items()).forEach {
            page.addItem(it)
        }

        page.addItem(InventoryItem(
            getItemStack = { page, item ->
                ItemStack.of(Material.ECHO_SHARD).apply {
                    setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, Unit.INSTANCE)
                    itemMeta = itemMeta.also {
                        it.itemModel = NamespacedKey(TreeTumblers.NAMESPACE, "timer")
                        it.isHideTooltip = true
                    }
                }
            },
            slot = 4
        ))
    }
}