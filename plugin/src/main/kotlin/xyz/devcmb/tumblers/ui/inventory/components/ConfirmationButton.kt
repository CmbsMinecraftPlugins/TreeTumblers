package xyz.devcmb.tumblers.ui.inventory.components

import com.noxcrew.interfaces.click.ClickContext
import com.noxcrew.interfaces.click.CompletableClickHandler
import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.interfaces.ChestInterfaceBuilder
import com.noxcrew.interfaces.properties.interfaceProperty
import com.noxcrew.noxesium.core.registry.CommonItemComponentTypes
import com.noxcrew.noxesium.paper.component.setNoxesiumComponent
import io.papermc.paper.util.Tick
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.buttonClickSound

fun ChestInterfaceBuilder.confirmationButton(point: GridPoint, type: ConfirmationButtonType, onClick: (it: ClickContext) -> Unit) {
    val isDownProperty = interfaceProperty(false)
    var isDown by isDownProperty

    withTransform(isDownProperty) { pane, view ->
        val onClick: CompletableClickHandler.(it: ClickContext) -> Unit = {
            TreeTumblers.pluginScope.launch {
                isDown = true
                view.player.buttonClickSound()

                delay(Tick.of(2))

                isDown = false
                onClick(it)
            }
        }

        val button = ItemStack.of(Material.ECHO_SHARD).apply {
            setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, com.noxcrew.noxesium.api.util.Unit.INSTANCE)
            itemMeta = itemMeta.also {
                it.itemName(type.itemName)
                it.itemModel = NamespacedKey(TreeTumblers.NAMESPACE,
                    if(isDown) type.clickedModel
                    else type.model
                )
            }
        }

        val empty = UserInterfaceUtility.empty()
        empty.editMeta {
            it.itemName(type.itemName)
        }

        pane[point] = StaticElement(drawable(button), onClick)
        pane[point.copy(y = point.y + 1)] = StaticElement(drawable(empty), onClick)
        pane[point.copy(y = point.y - 1)] = StaticElement(drawable(empty), onClick)
    }
}

fun ChestInterfaceBuilder.confirmationButton(x: Int, y: Int, type: ConfirmationButtonType, onClick: (it: ClickContext) -> Unit)
    = confirmationButton(GridPoint(x, y), type, onClick)

enum class ConfirmationButtonType(val itemName: Component, val model: String, val clickedModel: String) {
    YES(Format.mm("<green><b>Yes</b></green>"), "confirmation/yes_large", "confirmation/yes_large_clicked"),
    NO(Format.mm("<red><b>No</b></red>"),"confirmation/no_large", "confirmation/no_large_clicked")
}