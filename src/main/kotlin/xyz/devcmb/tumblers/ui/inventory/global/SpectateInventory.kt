package xyz.devcmb.tumblers.ui.inventory.global

import com.noxcrew.noxesium.core.registry.CommonItemComponentTypes
import com.noxcrew.noxesium.paper.component.setNoxesiumComponent
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
import xyz.devcmb.tumblers.ControllerRegistry
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.player.SpectatorController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.buttonClickSound
import xyz.devcmb.tumblers.util.tp
import xyz.devcmb.tumblers.util.tumblingPlayer

class SpectateInventory(
    val player: Player
) : HandledInventory {
    override val id: String = "spectateInventory"

    // TODO: Replace with custom inv instead of using glass panes
    override val inventory: ChestInventoryUI = ChestInventoryUI(player, Format.mm("<white>Spectate</white>"), 5).apply {
        val page = ChestInventoryPage()
        addPage("main", page, true)

        val itemMap = InventoryItemMap(
            getInventoryItems = { page, map ->
                val players: Set<Player> =
                    GameController.activeGame?.gameParticipants?.filter { it != player }?.toSet() ?: Team.entries
                        .filter { it.playingTeam }
                        .flatMap { it.getOnlinePlayers() }
                        .filter { it != player && it !in SpectatorController.spectators }
                        .toSet()

                val items: ArrayList<InventoryMappedItem> = ArrayList()
                players.forEach { plr ->
                    items.add(
                        InventoryMappedItem(
                        getItemStack = { page, item ->
                            ItemStack.of(Material.PLAYER_HEAD).apply {
                                setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, com.noxcrew.noxesium.api.util.Unit.INSTANCE)
                                itemMeta = itemMeta.also {
                                    it.itemName(Format.formatPlayerName(plr))
                                    it.itemModel = UserInterfaceUtility.FLAT_SKULL
                                    (it as SkullMeta).owningPlayer = plr

                                    it.lore(
                                        listOf(
                                            plr.tumblingPlayer.team.formattedName
                                        ).map { line -> line.decoration(TextDecoration.ITALIC, false) })
                                }
                            }
                        },
                        onClick = { page, item ->
                            if (!SpectatorController.spectators.contains(player)) return@InventoryMappedItem

                            if (SpectatorController.spectators.contains(plr)) {
                                player.sendMessage(Format.warning("This player can't be spectated right now."))
                                return@InventoryMappedItem
                            }

                            player.tp(plr.location)
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
            page.addItem(
                InventoryItem(
                    getItemStack = { page, item ->
                        ItemStack.of(Material.GRAY_STAINED_GLASS_PANE).apply {
                            setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, com.noxcrew.noxesium.api.util.Unit.INSTANCE)
                            itemMeta = itemMeta.also { meta ->
                                meta.isHideTooltip = true
                            }
                        }
                    },
                    it
                )
            )
        }

        page.addItem(
            InventoryItem(
            getItemStack = { page, item ->
                ItemStack.of(Material.ARROW).apply {
                    setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, com.noxcrew.noxesium.api.util.Unit.INSTANCE)
                    itemMeta = itemMeta.also {
                        it.itemName(Format.mm("<yellow>Previous Page</yellow>"))
                        it.lore(
                            listOf(
                                Format.mm("<white>Page ${itemMap.itemPage}</white>")
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        )
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

        page.addItem(
            InventoryItem(
            getItemStack = { page, item ->
                ItemStack.of(Material.ARROW).apply {
                    setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, com.noxcrew.noxesium.api.util.Unit.INSTANCE)
                    itemMeta = itemMeta.also {
                        it.itemName(Format.mm("<yellow>Next Page</yellow>"))
                        it.lore(
                            listOf(
                                Format.mm("<white>Page ${itemMap.itemPage}</white>")
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        )
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