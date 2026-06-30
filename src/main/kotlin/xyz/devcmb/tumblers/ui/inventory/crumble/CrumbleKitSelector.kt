package xyz.devcmb.tumblers.ui.inventory.crumble

import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.interfaces.buildChestInterface
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.TumblingUIException
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.controllers.games.crumble.Kit
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.buttonClickSound
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.wrapComponent

class CrumbleKitSelector : HandledInventory {
    override val id: String = "crumbleKitSelector"

    override val inventory = buildChestInterface {
        titleSupplier = {
            UserInterfaceUtility.customInventoryTitle(
                Component.text("\uEF02", NamedTextColor.WHITE).font(CrumbleController.font),
                Component.text("Kit Selector", NamedTextColor.WHITE)
            )
        }
        rows = 4

        val onSelect: (player: Player, kit: Kit) -> Unit = onSelect@{ player, kit ->
            val crumble = GameController.activeGame as? CrumbleController ?:
                throw TumblingUIException("Attempted to open the crumble kit selector while crumble was not active.")

            if(crumble.playerKits.filter { item -> item.value.id == kit.id }.size >= CrumbleController.maxPlayersPerKit) {
                player.sendMessage(Format.error("This kit has too many players!"))
                return@onSelect
            }

            if(crumble.playerKits[player.tumblingPlayer]?.id == kit.id) {
                player.sendMessage(Format.error("You've already selected this kit!"))
                return@onSelect
            }

            crumble.selectKit(player.tumblingPlayer, kit.id)
            player.buttonClickSound()
            UserInterfaceUtility.refreshAll(this@CrumbleKitSelector.id)
        }

        val slots = listOf(2, 5, 11, 14, 20, 23, 29, 32)
        withTransform { pane, view ->
            val crumble = GameController.activeGame as? CrumbleController ?:
                throw TumblingUIException("Attempted to open the crumble kit selector while crumble was not active.")

            slots.forEachIndexed { index, slot ->
                val (id, kit) = crumble.kitTemplates.toList().getOrNull(index) ?: return@forEachIndexed
                val item = ItemStack.of(Material.ECHO_SHARD).apply {
                    editMeta { meta ->
                        meta.itemName(Format.mm("<white>${kit.name}</white>"))
                        meta.itemModel = kit.inventoryModel
                        meta.lore(listOf(
                            Component.text("Ability: ${kit.abilityName}", NamedTextColor.AQUA),
                            *wrapComponent(
                                Component.text(kit.abilityDescription, NamedTextColor.WHITE),
                                40
                            ).toTypedArray(),
                            Component.empty(),
                            Component.text("Kill Power: ${kit.killPowerName}", NamedTextColor.YELLOW),
                            *wrapComponent(
                                Component.text(kit.killPowerDescription, NamedTextColor.WHITE),
                                40
                            ).toTypedArray(),
                        ).map { entry -> entry.decoration(TextDecoration.ITALIC, false) })
                    }
                }

                pane[GridPoint.fromBukkitChestSlot(slot)!!] = StaticElement(drawable(item)) {
                    val player = it.player
                    onSelect(player, kit)
                }

                repeat(CrumbleController.maxPlayersPerKit) {
                    val selectedPlayers = crumble.playerKits
                        .filter { entry ->
                            entry.key.team == view.player.tumblingPlayer.team && entry.value.id == id
                        }
                        .toList()

                    val selectedPlayer = selectedPlayers.getOrNull(it)
                    val item =
                        if(selectedPlayer == null)
                            ItemStack.of(Material.ECHO_SHARD).apply {
                                editMeta { meta ->
                                    meta.itemName(Format.mm("<green>Select this kit!</green>"))
                                    meta.itemModel = NamespacedKey(TreeTumblers.NAMESPACE, "empty")
                                }
                            }
                        else
                            ItemStack.of(Material.PLAYER_HEAD).apply {
                                editMeta { meta ->
                                    meta.itemModel = UserInterfaceUtility.FLAT_SKULL
                                    (meta as SkullMeta).owningPlayer = Bukkit.getOfflinePlayer(selectedPlayer.first.uuid)
                                    meta.itemName(Format.mm("<player:${selectedPlayer.first.uuid}>"))
                                }
                            }

                    pane[GridPoint.fromBukkitChestSlot(slot + it + 1)!!] = StaticElement(drawable(item)) { context ->
                        onSelect(context.player, kit)
                    }
                }
            }

        }
    }
}