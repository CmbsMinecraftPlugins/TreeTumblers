package xyz.devcmb.tumblers.ui.inventory

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.devcmb.invcontrol.chest.ChestInventoryPage
import xyz.devcmb.invcontrol.chest.ChestInventoryUI
import xyz.devcmb.invcontrol.chest.InventoryItem
import xyz.devcmb.tumblers.controllers.EventController
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.buttonClickSound

class ReadyCheckInventory(
    val player: Player,
    val eventController: EventController,
    override val id: String = "readyCheckInventory",
) : HandledInventory {
    override val inventory: ChestInventoryUI = ChestInventoryUI(
        player,
        Component.text("Are you ready?"),
        3
    ).apply {
        val page = ChestInventoryPage()
        addPage("main", page, true)

        page.addItem(InventoryItem(
            getItemStack = { page, item ->
                ItemStack.of(Material.LIME_CONCRETE).apply {
                    itemMeta = itemMeta.also {
                        it.itemName(Format.mm("<green><b>Ready!</b></green>"))
                    }
                }
            },
            slot = 12,
            onClick = { page, item ->
                player.buttonClickSound()
                page.ui.close()
                eventController.markReady(player)
            }
        ))

        page.addItem(InventoryItem(
            getItemStack = { page, item ->
                ItemStack.of(Material.RED_CONCRETE).apply {
                    itemMeta = itemMeta.also {
                        it.itemName(Format.mm("<red><b>Not Ready!</b></red>"))
                    }
                }
            },
            slot = 14,
            onClick = { page, item ->
                player.buttonClickSound()
                page.ui.close()
                eventController.markNotReady(player)
            }
        ))
    }
}