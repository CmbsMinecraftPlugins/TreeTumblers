package xyz.devcmb.tumblers.ui.inventory.crumble

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.devcmb.invcontrol.chest.ChestInventoryPage
import xyz.devcmb.invcontrol.chest.ChestInventoryUI
import xyz.devcmb.invcontrol.chest.map.InventoryItemMap
import xyz.devcmb.invcontrol.chest.map.InventoryMappedItem
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.buttonClickSound

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
                crumble.kitTemplates.forEach { _,template ->
                    items.add(InventoryMappedItem(
                        getItemStack = { _,_ ->
                            val currentPlayerKits = crumble.playerKits.filter { item -> item.value.id == template.id }
                            var stack = ItemStack.of(Material.PAPER).apply {
                                itemMeta = itemMeta.also { meta ->
                                    meta.itemName(Component.text(template.name))
                                    if(
                                        crumble.playerKits[player]?.id != template.id
                                        && currentPlayerKits.size < CrumbleController.maxPlayersPerKit
                                    ) meta.itemModel = template.inventoryModel
                                }
                            }

                            var lore: MutableList<Component> = mutableListOf(
                                Component.text("Ability: ${template.abilityName}", NamedTextColor.AQUA),
                                *MiscUtils.wrapComponent(
                                    Component.text(template.abilityDescription, NamedTextColor.WHITE),
                                    40
                                ).toTypedArray(),
                                Component.empty(),
                                Component.text("Kill Power: ${template.killPowerName}", NamedTextColor.YELLOW),
                                *MiscUtils.wrapComponent(
                                    Component.text(template.killPowerDescription, NamedTextColor.WHITE),
                                    40
                                ).toTypedArray(),
                                Component.empty(),
                                Component.text("${currentPlayerKits.size}/${CrumbleController.maxPlayersPerKit}", NamedTextColor.GRAY)
                            )

                            if(crumble.playerKits[player]?.id == template.id) {
                                stack = stack.withType(Material.GREEN_STAINED_GLASS_PANE)
                                lore.add(Component.text("Selected!", NamedTextColor.GREEN))
                            }

                            if(crumble.playerKits.filter { item -> template.id == item.value.id }.size >= CrumbleController.maxPlayersPerKit) {
                                if(stack.type != Material.GREEN_STAINED_GLASS_PANE) stack = stack.withType(Material.BARRIER)
                                lore.add(Component.text("Max players!", NamedTextColor.RED))
                            }

                            lore = lore.map { it.decoration(TextDecoration.ITALIC, false) }.toMutableList()
                            stack = stack.apply {
                                itemMeta = itemMeta.also {
                                    it.lore(lore)
                                }
                            }

                            stack
                        },
                        onClick = { page, item ->
                            crumble.selectKit(player, template.id)
                            player.buttonClickSound()
                            UserInterfaceUtility.refreshAll(id)
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