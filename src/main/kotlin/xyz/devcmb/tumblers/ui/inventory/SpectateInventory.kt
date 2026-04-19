package xyz.devcmb.tumblers.ui.inventory

import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import xyz.devcmb.invcontrol.chest.ChestInventoryPage
import xyz.devcmb.invcontrol.chest.ChestInventoryUI
import xyz.devcmb.invcontrol.chest.InventoryItem
import xyz.devcmb.invcontrol.chest.map.InventoryItemMap
import xyz.devcmb.invcontrol.chest.map.InventoryMappedItem
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.SpectatorController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.buttonClickSound
import xyz.devcmb.tumblers.util.tumblingPlayer

class SpectateInventory(
    val player: Player,
    val gameController: GameController,
    override val id: String = "spectateInventory",
) : HandledInventory {
    val spectateController by lazy {
        ControllerDelegate.getController<SpectatorController>()
    }
    override val inventory: ChestInventoryUI = ChestInventoryUI(player, Format.mm("Spectate"), 5).apply {
        val page = ChestInventoryPage()
        addPage("main", page, true)

        val itemMap = InventoryItemMap(
            getInventoryItems = { page, map ->
                val players: Set<Player> = gameController.activeGame?.gameParticipants?.filter { it != player }?.toSet() ?:
                    Team.entries
                        .filter { it.playingTeam }
                        .flatMap { it.getOnlinePlayers() }
                        .filter { it != player && it !in spectateController.spectators }
                        .toSet()

                val items: ArrayList<InventoryMappedItem> = ArrayList()
                players.forEach { plr ->
                    items.add(InventoryMappedItem(
                        getItemStack = { page, item ->
                            ItemStack.of(Material.PLAYER_HEAD).apply {
                                itemMeta = itemMeta.also {
                                    it.itemName(Format.formatPlayerName(plr))
                                    it.itemModel = UserInterfaceUtility.FLAT_SKULL
                                    (it as SkullMeta).owningPlayer = plr

                                    it.lore(listOf(
                                        plr.tumblingPlayer.team.formattedName
                                    ).map { line -> line.decoration(TextDecoration.ITALIC, false) })
                                }
                            }
                        },
                        onClick = { page, item ->
                            if(!spectateController.spectators.contains(player)) return@InventoryMappedItem

                            if(spectateController.spectators.contains(plr)) {
                                player.sendMessage(Format.warning("This player can't be spectated right now."))
                                return@InventoryMappedItem
                            }

                            player.teleport(plr.location)
                            page.ui.close()
                        }
                    ))
                }
                items
            },
            startSlot = 0,
            maxItems = 27,
            itemPage = 1
        )
        page.addItemMap(itemMap)

        (27..35).forEach {
            page.addItem(InventoryItem(
                getItemStack = { page, item ->
                    ItemStack.of(Material.GRAY_STAINED_GLASS_PANE)
                },
                it
            ))
        }

        page.addItem(InventoryItem(
            getItemStack = { page, item ->
                ItemStack.of(Material.ARROW).apply {
                    itemMeta = itemMeta.also {
                        it.itemName(Format.mm("<yellow>Previous Page</yellow>"))
                    }
                }
            },
            slot = 36,
            onClick = { page, item ->
                player.buttonClickSound()
                itemMap.pageBack()
                page.reload()
            }
        ))

        page.addItem(InventoryItem(
            getItemStack = { page, item ->
                ItemStack.of(Material.ARROW).apply {
                    itemMeta = itemMeta.also {
                        it.itemName(Format.mm("<yellow>Next Page</yellow>"))
                    }
                }
            },
            slot = 44,
            onClick = { page, item ->
                player.buttonClickSound()
                itemMap.pageForward()
                page.reload()
            }
        ))
    }
}