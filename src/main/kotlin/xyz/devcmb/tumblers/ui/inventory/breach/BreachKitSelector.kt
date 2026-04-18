package xyz.devcmb.tumblers.ui.inventory.breach

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.devcmb.invcontrol.chest.ChestInventoryPage
import xyz.devcmb.invcontrol.chest.ChestInventoryUI
import xyz.devcmb.invcontrol.chest.InventoryItem
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.games.breach.BreachController
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.buttonClickSound

class BreachKitSelector(
    val player: Player,
    val gameController: GameController,
    override val id: String = "breachKitSelector"
) : HandledInventory {
    val kitItems: HashMap<Material, Int> = hashMapOf(
        Material.BOW to 11,
        Material.CROSSBOW to 13,
        Material.TRIDENT to 15
    )

    override val inventory = ChestInventoryUI(
        player,
        Format.mm("Pick your weapon."),
        3
    ).apply {
        val page = ChestInventoryPage()
        this.addPage("main", page, true)

        kitItems.forEach { (material, slot) ->
            page.addItem(InventoryItem(
                getItemStack = { page, item ->
                    val itemStack = ItemStack.of(material)

                    itemStack
                },

                onClick = { page, item ->
                    val breach = gameController.activeGame as BreachController

                    breach.giveWeapon(player, material)
                    player.buttonClickSound()
                },

                slot = slot
            ))
        }
    }
}