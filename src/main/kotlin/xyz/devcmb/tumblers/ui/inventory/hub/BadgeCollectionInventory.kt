package xyz.devcmb.tumblers.ui.inventory.hub

import com.noxcrew.noxesium.api.util.Unit
import com.noxcrew.noxesium.core.registry.CommonItemComponentTypes
import com.noxcrew.noxesium.paper.component.setNoxesiumComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.devcmb.invcontrol.chest.ChestInventoryPage
import xyz.devcmb.invcontrol.chest.ChestInventoryUI
import xyz.devcmb.invcontrol.chest.InventoryItem
import xyz.devcmb.invcontrol.chest.map.InventoryItemMap
import xyz.devcmb.invcontrol.chest.map.InventoryMappedItem
import xyz.devcmb.tumblers.controllers.event.BadgeController
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.buttonClickSound
import xyz.devcmb.tumblers.util.tumblingPlayer
import java.text.SimpleDateFormat

class BadgeCollectionInventory(
    val player: Player,
    val badgeController: BadgeController,
    val gameController: GameController,
    override val id: String = "badgeCollectionInventory",
) : HandledInventory {
    @Suppress("UnstableApiUsage")
    override val inventory: ChestInventoryUI = ChestInventoryUI(
        player,
        rows = 6,
        title = UserInterfaceUtility.negativeSpace(8)
            .append(Component.text("\uEF00", NamedTextColor.WHITE).font(NamespacedKey("tumbling", "collection")))
            .append(UserInterfaceUtility.negativeSpace(UserInterfaceUtility.FULL_INVENTORY_NEGATIVE_ADVANCE))
            .append(Component.text("Badge Collection", NamedTextColor.WHITE).font(NamespacedKey("minecraft", "default"))),
    ).apply {
        val page = ChestInventoryPage()
        addPage("main", page, true)

        var selectedGameIndex: Int? = null
        var selectedGame: GameController.RegisteredGame? = null

        // Game Selection
        val gameSelectionMap = InventoryItemMap(
            getInventoryItems = { page, map ->
                ArrayList(gameController.games.mapIndexed { index, game ->
                    InventoryMappedItem(
                        getItemStack = { page, item ->
                            ItemStack.of(Material.ECHO_SHARD).apply {
                                setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, Unit.INSTANCE)
                                itemMeta = itemMeta.also {
                                    it.itemName(Component.empty())
                                    it.lore(listOf(
                                        Component.empty(),
                                        game.logo.color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
                                    ))
                                    it.itemModel = NamespacedKey("tumbling", "games/${game.id}")
                                }
                            }
                        },
                        onClick = { page, item ->
                            if(selectedGameIndex == index) return@InventoryMappedItem

                            selectedGameIndex = index
                            selectedGame = game
                            player.buttonClickSound()
                            page.reload()
                        }
                    )
                })
            },
            startSlot = 0,
            maxItems = 8,
            itemPage = 1,
            slots = ArrayList((0 until 4).flatMap { row ->
                val startSlot = row * 9 + 1
                arrayListOf(startSlot, startSlot + 1)
            })
        )
        page.addItemMap(gameSelectionMap)

        page.addItem(InventoryItem(
            getItemStack = { page, item ->
                if(selectedGameIndex == null || selectedGameIndex % 2 != 0)
                    return@InventoryItem ItemStack.empty()

                ItemStack.of(Material.PAPER).apply {
                    setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, Unit.INSTANCE)
                    itemMeta = itemMeta.also {
                        it.isHideTooltip = true
                        it.itemModel = NamespacedKey("tumbling", "selection_outline")

                        val component = it.customModelDataComponent
                        component.strings = listOf((4 - (selectedGameIndex / 2)).toString())

                        it.setCustomModelDataComponent(component)
                    }
                }
            },
            slot = 37
        ))

        page.addItem(InventoryItem(
            getItemStack = { page, item ->
                if(selectedGameIndex == null || selectedGameIndex % 2 == 0)
                    return@InventoryItem ItemStack.empty()

                ItemStack.of(Material.PAPER).apply {
                    setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, Unit.INSTANCE)
                    itemMeta = itemMeta.also {
                        it.isHideTooltip = true
                        it.itemModel = NamespacedKey("tumbling", "selection_outline")

                        val component = it.customModelDataComponent
                        component.strings = listOf((4 - (selectedGameIndex / 2)).toString())

                        it.setCustomModelDataComponent(component)
                    }
                }
            },
            slot = 38
        ))

        val gameBadgesMap = InventoryItemMap(
            getInventoryItems = { page, map ->
                if(selectedGame == null || selectedGame.badges == null) return@InventoryItemMap arrayListOf()

                val playerBadges = player.tumblingPlayer.badges
                return@InventoryItemMap ArrayList(selectedGame.badges!!.map { badge ->
                    InventoryMappedItem(
                        getItemStack = { page, item ->
                            if(badge !in playerBadges.keys) {
                                ItemStack.of(Material.ECHO_SHARD).apply {
                                    setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, Unit.INSTANCE)
                                    itemMeta = itemMeta.also {
                                        it.itemName(Format.mm("<gold>${badge.badgeName}</gold>"))
                                        it.itemModel = NamespacedKey("tumbling", "lock")
                                        it.lore(listOf(
                                            Component.empty(),
                                            Format.mm("<red>Locked!</red>"),
                                            Component.empty(),
                                            *MiscUtils.wrapComponent(Component.text(badge.hint, NamedTextColor.WHITE), 30).toTypedArray()
                                        ).map { entry ->
                                            entry.decoration(TextDecoration.ITALIC, false)
                                        })
                                    }
                                }
                            } else {
                                val timestamp = playerBadges[badge]!!
                                ItemStack.of(Material.LIME_STAINED_GLASS_PANE).apply {
                                    setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, Unit.INSTANCE)
                                    itemMeta = itemMeta.also {
                                        it.itemName(Format.mm("<gold>${badge.badgeName}</gold>"))
                                        it.lore(listOf(
                                            Component.empty(),
                                            Format.mm("<green>Unlocked on ${SimpleDateFormat("EEE MMM d, yyyy ").format(timestamp.time)}</green>"),
                                            Component.empty(),
                                            *MiscUtils.wrapComponent(Component.text(badge.hint, NamedTextColor.WHITE), 30).toTypedArray()
                                        ).map { entry ->
                                            entry.decoration(TextDecoration.ITALIC, false)
                                        })
                                    }
                                }
                            }
                        }
                    )
                })
            },
            startSlot = 0,
            itemPage = 1,
            maxItems = 20,
            slots = ArrayList((0..<5).flatMap { row ->
                val startSlot = row * 9 + 4
                arrayListOf(*(startSlot..startSlot + 4).toList().toTypedArray())
            })
        )

        page.addItemMap(gameBadgesMap)
    }
}