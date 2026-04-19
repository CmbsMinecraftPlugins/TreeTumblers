package xyz.devcmb.tumblers.ui.inventory.breach

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.devcmb.invcontrol.chest.ChestInventoryPage
import xyz.devcmb.invcontrol.chest.ChestInventoryUI
import xyz.devcmb.invcontrol.chest.InventoryItem
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.games.breach.BreachController
import xyz.devcmb.tumblers.controllers.games.breach.BreachKit
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.buttonClickSound

class BreachKitSelector(
    val player: Player,
    val gameController: GameController,
    override val id: String = "breachKitSelector"
) : HandledInventory {


    override val inventory = ChestInventoryUI(
        player,
        Format.mm("Pick your weapon."),
        3
    ).apply {
        val page = ChestInventoryPage()
        this.addPage("main", page, true)



        val kitItems: HashMap<BreachKit, Int> = hashMapOf(
            BreachKit.BOW to 11,
            BreachKit.CROSSBOW to 13,
            BreachKit.TRIDENT to 15
        )

        kitItems.forEach { (kit, slot) ->
            page.addItem(InventoryItem(
                getItemStack = { page, item ->
                    val itemStack = ItemStack.of(kit.item).apply {
                        itemMeta = itemMeta.also { meta ->
                            meta.itemName(kit.label)
                            meta.lore(kit.description)
                        }
                    }

                    itemStack
                },

                onClick = { page, item ->
                    val breach = gameController.activeGame as BreachController
                    breach.giveKit(player, kit)
                    player.buttonClickSound()
                },

                slot = slot
            ))
        }
    }
}