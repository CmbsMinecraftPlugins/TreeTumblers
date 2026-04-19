package xyz.devcmb.tumblers.ui.inventory.breach

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.devcmb.invcontrol.chest.ChestInventoryPage
import xyz.devcmb.invcontrol.chest.ChestInventoryUI
import xyz.devcmb.invcontrol.chest.InventoryItem
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.games.breach.BreachController
import xyz.devcmb.tumblers.controllers.games.breach.BreachKit
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.buttonClickSound
import xyz.devcmb.tumblers.util.tumblingPlayer

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

        page.addItem(InventoryItem(
            getItemStack = { page, item ->
                if(gameController.activeGame?.id != "breach") {
                    DebugUtil.severe("Attempted to load BreachKitSelector while the game is not active")
                    return@InventoryItem ItemStack.of(Material.AIR)
                }

                val breach = gameController.activeGame as BreachController
                val team = player.tumblingPlayer.team

                var itemHolder: Player? = null

                if (team == breach.playingTeams.first) {
                    itemHolder = breach.team1holder
                } else if (team == breach.playingTeams.second) {
                    itemHolder = breach.team2holder
                }

                val itemStack = ItemStack.of(Material.NETHER_STAR).apply {
                    itemMeta = itemMeta.also { meta ->
                        if (itemHolder == null) {
                            meta.itemName(Format.mm("<light_purple>Hold the Star"))
                            meta.lore(listOf(
                                Format.mm("<aqua>Keep it safe."),
                                Format.mm("<aqua>Your victory depends on it.")
                            ))
                        } else if (itemHolder != player) {
                            meta.itemName(Format.mm("<dark_purple>Star held by ${itemHolder.name}"))
                            meta.lore(listOf(
                                Format.mm("<aqua>Keep ${itemHolder.name} safe."),
                                Format.mm("<aqua>Your victory depends on it.")
                            ))
                        } else if (itemHolder == player) {
                            meta.itemName(Format.mm("<dark_purple>You hold the Star"))
                            meta.lore(listOf(
                                Format.mm("<aqua>Keep it, and yourself safe."),
                                Format.mm("<aqua>Your victory depends on it.")
                            ))
                        }
                    }
                }

                itemStack
            },

            onClick = { page, item ->
                val breach = gameController.activeGame as BreachController
                breach.takeItem(player)
                player.buttonClickSound()
                UserInterfaceUtility.refreshAll(id)
            },

            slot = 22
        ))
    }
}