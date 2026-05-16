package xyz.devcmb.tumblers.ui.inventory.components

import com.noxcrew.noxesium.core.registry.CommonItemComponentTypes
import com.noxcrew.noxesium.paper.component.setNoxesiumComponent
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.devcmb.invcontrol.chest.ChestInventoryPage
import xyz.devcmb.invcontrol.chest.InventoryItem
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.TumblingInterfaceException
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.buttonClickSound
import xyz.devcmb.tumblers.util.runTaskLater

class ConfirmationButton(
    val buttonType: ConfirmationButtonType,
    val slot: Int,
    val onClick: (page: ChestInventoryPage, item: InventoryItem) -> Unit,
) {
    var isDown: Boolean = false
    init {
        val slotColumn = (slot % 9) + 1
        if(slotColumn == 1 || slotColumn == 9) {
            throw TumblingInterfaceException("Confirmation button may not be at the edge of the inventory panel")
        }
    }

    fun items(): List<InventoryItem> {
        val onClick: ((page: ChestInventoryPage, item: InventoryItem) -> Unit) = { page, item ->
            isDown = true
            page.reload()
            page.ui.player.buttonClickSound()
            runTaskLater(2) {
                isDown = false
                page.reload()
                this.onClick.invoke(page, item)
            }
        }

        return listOf(
            InventoryItem(
                getItemStack = { page, item ->
                    ItemStack.of(Material.ECHO_SHARD).apply {
                        setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, com.noxcrew.noxesium.api.util.Unit.INSTANCE)
                        itemMeta = itemMeta.also {
                            it.itemName(buttonType.itemName)
                            it.itemModel = NamespacedKey(TreeTumblers.NAMESPACE,
                                if(isDown) buttonType.clickedModel
                                else buttonType.model
                            )
                        }
                    }
                },
                slot,
                onClick
            ),
            InventoryItem(
                getItemStack = { page, item ->
                    ItemStack.of(Material.ECHO_SHARD).apply {
                        setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, com.noxcrew.noxesium.api.util.Unit.INSTANCE)
                        itemMeta = itemMeta.also {
                            it.itemName(buttonType.itemName)
                            it.itemModel = NamespacedKey(TreeTumblers.NAMESPACE, "empty")
                        }
                    }
                },
                slot - 1,
                onClick
            ),
            InventoryItem(
                getItemStack = { page, item ->
                    ItemStack.of(Material.ECHO_SHARD).apply {
                        setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, com.noxcrew.noxesium.api.util.Unit.INSTANCE)
                        itemMeta = itemMeta.also {
                            it.itemName(buttonType.itemName)
                            it.itemModel = NamespacedKey(TreeTumblers.NAMESPACE, "empty")
                        }
                    }
                },
                slot + 1,
                onClick
            )
        )
    }

    enum class ConfirmationButtonType(val itemName: Component, val model: String, val clickedModel: String) {
        YES(Format.mm("<green><b>Yes</b></green>"), "confirmation/yes_large", "confirmation/yes_large_clicked"),
        NO(Format.mm("<green><b>No</b></green>"),"confirmation/no_large", "confirmation/no_large_clicked")
    }
}