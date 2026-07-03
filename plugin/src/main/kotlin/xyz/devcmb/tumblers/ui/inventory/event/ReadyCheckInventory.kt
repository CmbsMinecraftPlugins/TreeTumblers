package xyz.devcmb.tumblers.ui.inventory.event

import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.noxesium.api.util.Unit
import com.noxcrew.noxesium.core.registry.CommonItemComponentTypes
import com.noxcrew.noxesium.paper.component.setNoxesiumComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.event.EventController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.ui.inventory.components.ConfirmationButtonType
import xyz.devcmb.tumblers.ui.inventory.components.confirmationButton
import xyz.devcmb.tumblers.util.Font

class ReadyCheckInventory : HandledInventory {
    override val id: String = "readyCheckInventory"

    override val inventory = buildChestInterface {
        titleSupplier = {
            UserInterfaceUtility.customInventoryTitle(
                Font.getGlyph("container/filled_9"),
                Component.text("Are you ready?", NamedTextColor.WHITE)
            )
        }
        rows = 1

        confirmationButton(0, 2, ConfirmationButtonType.YES) {
            it.view.close(TreeTumblers.pluginScope)
            EventController.markReady(it.view.player)
        }

        confirmationButton(0, 6, ConfirmationButtonType.NO) {
            it.view.close(TreeTumblers.pluginScope)
            EventController.markNotReady(it.view.player)
        }

        withTransform { pane, _ ->
            pane[0,4] = StaticElement(drawable(ItemStack.of(Material.ECHO_SHARD).apply {
                setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, Unit.INSTANCE)
                itemMeta = itemMeta.also {
                    it.itemModel = NamespacedKey(TreeTumblers.NAMESPACE, "icon/timer")
                    it.isHideTooltip = true
                }
            }))
        }
    }
}