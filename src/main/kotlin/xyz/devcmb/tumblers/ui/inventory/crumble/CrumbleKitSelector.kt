package xyz.devcmb.tumblers.ui.inventory.crumble

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.devcmb.invcontrol.chest.ChestInventoryPage
import xyz.devcmb.invcontrol.chest.ChestInventoryUI
import xyz.devcmb.invcontrol.chest.map.InventoryItemMap
import xyz.devcmb.invcontrol.chest.map.InventoryMappedItem
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController.Companion.maxPlayersPerKit
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.DebugUtil

class CrumbleKitSelector(
    val player: Player,
    val gameController: GameController,
    override val id: String = "crumbleKitSelector"
) : HandledInventory {
    override val inventory = ChestInventoryUI(player, Component.text("Kit Selector")).apply {
        val page = ChestInventoryPage()
        addPage("main", page, true)
        page.addItemMap(InventoryItemMap(
            getInventoryItems = { page, ui ->
                if(gameController.activeGame?.id != "crumble") {
                    DebugUtil.severe("Attempted to load CrumbleKitSelector while the game is not active")
                    return@InventoryItemMap arrayListOf()
                }

                val crumble = gameController.activeGame as CrumbleController

                val items: ArrayList<InventoryMappedItem> = ArrayList()
                crumble.registeredKits.forEach {
                    items.add(InventoryMappedItem(
                        getItemStack = { _,_ ->
                            val currentPlayerKits = crumble.playerKits.filter { item -> item.value == it }
                            var stack = ItemStack.of(Material.PAPER).apply {
                                itemMeta = itemMeta.also { meta ->
                                    meta.itemName(Component.text(it.name))
                                    if(
                                        crumble.playerKits[player] != it
                                        && currentPlayerKits.size < maxPlayersPerKit
                                    ) meta.itemModel = it.inventoryModel
                                }
                            }

                            if(crumble.playerKits[player] == it) {
                                stack = stack.withType(Material.GREEN_STAINED_GLASS_PANE)
                            } else if(crumble.playerKits.filter { item -> it == item }.size >= maxPlayersPerKit) {
                                stack = stack.withType(Material.BARRIER)
                            }

                            stack
                        },
                        onClick = { page, item ->
                            crumble.playerKits.put(player, it)
                            reload()
                        }
                    ))
                }
                items
            },
            startSlot = 0,
            itemPage = 1,
            maxItems = 27
        ))
    }
}