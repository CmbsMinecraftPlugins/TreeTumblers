package xyz.devcmb.tumblers.ui.inventory.crumble

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import xyz.devcmb.invcontrol.chest.ChestInventoryPage
import xyz.devcmb.invcontrol.chest.ChestInventoryUI
import xyz.devcmb.invcontrol.chest.InventoryItem
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.buttonClickSound
import xyz.devcmb.tumblers.util.tumblingPlayer

class CrumbleKitSelector(
    val player: Player,
    val gameController: GameController,
    override val id: String = "crumbleKitSelector"
) : HandledInventory {
    // https://septicuss.notion.site/Fonts-ce8c8c12c313463ea01ac9b16d7e6bbb
    override val inventory = ChestInventoryUI(
        player,
        UserInterfaceUtility.negativeSpace(8)
            .append(Component.text("\uEF02", NamedTextColor.WHITE).font(CrumbleController.font))
            .append(UserInterfaceUtility.negativeSpace(UserInterfaceUtility.FULL_INVENTORY_NEGATIVE_ADVANCE))
            .append(Component.text("Kit Selector", NamedTextColor.WHITE).font(NamespacedKey("minecraft", "default"))),
        4
    ).apply {
        val page = ChestInventoryPage()
        addPage("main", page, true)

        val slots = listOf(2, 5, 11, 14, 20, 23, 29, 32)
        val onClick: ((id: String) -> Unit) = onClick@{ id ->
            val crumble = gameController.activeGame as? CrumbleController ?: return@onClick

            if(crumble.playerKits.filter { item -> item.value.id == id }.size >= CrumbleController.maxPlayersPerKit) {
                player.sendMessage(Format.error("This kit has too many players!"))
                return@onClick
            }

            if(crumble.playerKits[player.tumblingPlayer]?.id == id) {
                player.sendMessage(Format.error("You've already selected this kit!"))
                return@onClick
            }

            crumble.selectKit(player, id)
            player.buttonClickSound()
            UserInterfaceUtility.refreshAll(this@CrumbleKitSelector.id)
        }

        slots.forEachIndexed { index, slot ->
            page.addItem(InventoryItem(
                getItemStack = { page, item ->
                    val crumbleController = gameController.activeGame as? CrumbleController ?: return@InventoryItem ItemStack.empty()
                    val (id, kit) = crumbleController.kitTemplates.toList().getOrNull(index) ?: return@InventoryItem ItemStack.empty()

                    ItemStack.of(Material.ECHO_SHARD).apply {
                        editMeta { meta ->
                            meta.itemName(Format.mm("<white>${kit.name}</white>"))
                            meta.itemModel = kit.inventoryModel
                            meta.lore(listOf(
                                Component.text("Ability: ${kit.abilityName}", NamedTextColor.AQUA),
                                *MiscUtils.wrapComponent(
                                    Component.text(kit.abilityDescription, NamedTextColor.WHITE),
                                    40
                                ).toTypedArray(),
                                Component.empty(),
                                Component.text("Kill Power: ${kit.killPowerName}", NamedTextColor.YELLOW),
                                *MiscUtils.wrapComponent(
                                    Component.text(kit.killPowerDescription, NamedTextColor.WHITE),
                                    40
                                ).toTypedArray(),
                            ).map { entry -> entry.decoration(TextDecoration.ITALIC, false) })
                        }
                    }
                },
                onClick = { page, item ->
                    val crumbleController = gameController.activeGame as? CrumbleController ?: return@InventoryItem
                    val (id, kit) = crumbleController.kitTemplates.toList().getOrNull(index) ?: return@InventoryItem

                    onClick(id)
                },
                slot = slot
            ))

            repeat(CrumbleController.maxPlayersPerKit) { pIndex ->
                page.addItem(InventoryItem(
                    getItemStack = { page, item ->
                        val crumbleController = gameController.activeGame as? CrumbleController ?: return@InventoryItem ItemStack.empty()
                        val (id, _) = crumbleController.kitTemplates.toList().getOrNull(index) ?: return@InventoryItem ItemStack.empty()

                        val selectedPlayers = crumbleController.playerKits
                            .filter {
                                it.key.team == player.tumblingPlayer.team
                                && it.value.id == id
                            }
                            .toList()

                        val (player, _) = selectedPlayers.getOrNull(pIndex) ?: return@InventoryItem ItemStack.of(Material.ECHO_SHARD).apply {
                            editMeta { meta ->
                                meta.itemName(Format.mm("<green>Select this kit!</green>"))
                                meta.itemModel = NamespacedKey(TreeTumblers.NAMESPACE, "empty")
                            }
                        }

                        ItemStack.of(Material.PLAYER_HEAD).apply {
                            editMeta { meta ->
                                meta.itemName(Format.mm("<player:${player.uuid}>"))
                                meta.itemModel = UserInterfaceUtility.FLAT_SKULL
                                (meta as SkullMeta).owningPlayer = Bukkit.getOfflinePlayer(player.uuid)
                            }
                        }
                    },
                    slot = slot + (pIndex + 1),
                    onClick = { page, item ->
                        val crumbleController = gameController.activeGame as? CrumbleController ?: return@InventoryItem
                        val (id, kit) = crumbleController.kitTemplates.toList().getOrNull(index) ?: return@InventoryItem

                        onClick(id)
                    },
                ))
            }
        }
    }
}