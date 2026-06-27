package xyz.devcmb.tumblers.ui.inventory.hub

import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.properties.interfaceProperty
import com.noxcrew.noxesium.api.util.Unit
import com.noxcrew.noxesium.core.registry.CommonItemComponentTypes
import com.noxcrew.noxesium.paper.component.setNoxesiumComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.buttonClickSound
import xyz.devcmb.tumblers.util.forEachInGridIndexed
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.wrapComponent
import java.text.SimpleDateFormat

object BadgeCollectionInventory : HandledInventory {
    override val id: String = "badgeCollectionInventory"

    @Suppress("UnstableApiUsage")
    override val inventory = buildChestInterface {
        titleSupplier = { UserInterfaceUtility.customInventoryTitle(
            Component.text("\uEF00", NamedTextColor.WHITE).font(NamespacedKey(TreeTumblers.NAMESPACE, "collection")),
            Component.text("Badge Collection", NamedTextColor.WHITE)
        ) }
        rows = 6

        val selectedGameProperty = interfaceProperty(SelectedGame(null, null))
        var selectedGame by selectedGameProperty

        // Game Selector
        withTransform(selectedGameProperty) { pane, view ->
            forEachInGridIndexed(4, 2) { index, row, col ->
                val game = GameController.games.getOrNull(index) ?: return@forEachInGridIndexed
                val item = ItemStack.of(Material.ECHO_SHARD).apply {
                    setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, Unit.INSTANCE)
                    itemMeta = itemMeta.also {
                        it.itemName(Component.empty())
                        it.lore(listOf(
                            Component.empty(),
                            game.data.logo.color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
                        ))
                        it.itemModel = NamespacedKey(TreeTumblers.NAMESPACE, "games/${game.data.id}")
                    }
                }

                pane[row, col + 1] = StaticElement(drawable(item)) {
                    selectedGame = SelectedGame(index, game)
                    it.player.buttonClickSound()
                }
            }


            if(selectedGame.index != null) {
                val selectionOutline = ItemStack.of(Material.PAPER).apply {
                    setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, Unit.INSTANCE)
                    itemMeta = itemMeta.also {
                        it.isHideTooltip = true
                        it.itemModel = NamespacedKey(TreeTumblers.NAMESPACE, "selection_outline")

                        val component = it.customModelDataComponent
                        component.strings = listOf((4 - (selectedGame.index!! / 2)).toString())

                        it.setCustomModelDataComponent(component)
                    }
                }

                if(selectedGame.index!! % 2 == 0) pane[4, 1] = StaticElement(drawable(selectionOutline))
                else pane[4, 2] = StaticElement(drawable(selectionOutline))
            }
        }

        // Badge Section
        withTransform(selectedGameProperty) { pane, view ->
            if(selectedGame.index != null && selectedGame.game != null) {
                val playerBadges = view.player.tumblingPlayer.badges
                forEachInGridIndexed(5, 4) { index, row, col ->
                    val badge = selectedGame.game!!.data.badges?.getOrNull(index) ?: return@forEachInGridIndexed

                    val item = if(badge !in playerBadges.keys) {
                        ItemStack.of(Material.ECHO_SHARD).apply {
                            setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, Unit.INSTANCE)
                            itemMeta = itemMeta.also {
                                it.itemName(Format.mm("<gold>${badge.badgeName}</gold>"))
                                it.itemModel = NamespacedKey(TreeTumblers.NAMESPACE, "lock")
                                it.lore(listOf(
                                    Component.empty(),
                                    Format.mm("<red>Locked!</red>"),
                                    Component.empty(),
                                    *wrapComponent(Component.text(badge.hint, NamedTextColor.WHITE), 30).toTypedArray()
                                ).map { entry ->
                                    entry.decoration(TextDecoration.ITALIC, false)
                                })
                            }
                        }
                    } else {
                        val timestamp = playerBadges[badge]!!
                        ItemStack.of(Material.ECHO_SHARD).apply {
                            setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, Unit.INSTANCE)
                            itemMeta = itemMeta.also {
                                it.itemName(Format.mm("<gold>${badge.badgeName}</gold>"))
                                it.itemModel = NamespacedKey(TreeTumblers.NAMESPACE, "badges/${badge.game}/${badge.name.lowercase()}")
                                it.lore(listOf(
                                    Component.empty(),
                                    Format.mm("<green>Unlocked on ${SimpleDateFormat("EEE MMM d, yyyy ").format(timestamp.time)}</green>"),
                                    Component.empty(),
                                    *wrapComponent(Component.text(badge.hint, NamedTextColor.WHITE), 30).toTypedArray()
                                ).map { entry ->
                                    entry.decoration(TextDecoration.ITALIC, false)
                                })
                            }
                        }
                    }

                    pane[row, col + 4] = StaticElement(drawable(item))
                }
            }
        }
    }

    private data class SelectedGame(
        val index: Int?,
        val game: GameController.RegisteredGame?
    )
}