package xyz.devcmb.tumblers.ui.inventory.global

import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.properties.interfaceProperty
import com.noxcrew.noxesium.core.registry.CommonItemComponentTypes
import com.noxcrew.noxesium.paper.component.setNoxesiumComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.player.SpectatorController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tp
import xyz.devcmb.tumblers.util.tumblingPlayer

object SpectateInventory : HandledInventory {
    override val id: String = "spectateInventory"

    override val inventory = buildChestInterface {
        titleSupplier = { Component.text("Spectate", NamedTextColor.WHITE) }
        rows = 5

        val pageProperty = interfaceProperty(1)
        var page by pageProperty

        withTransform(pageProperty) { pane, view ->
            val player = view.player
            val players: List<Player> =
                GameController.activeGame
                    ?.gameParticipants
                    ?.mapNotNull { it.bukkitPlayer }
                    ?.filter { it != player.tumblingPlayer }
                ?: Team.entries
                    .filter { it.playingTeam }
                    .flatMap { it.getOnlinePlayers() }
                    .filter { it != player && it !in SpectatorController.spectators }

            val startIndex = (page - 1) * 27
            val pagePlayers = players.subList(startIndex, minOf(startIndex + 27, players.size))
            val maxPage = (players.size / 27) + 1

            pagePlayers.forEachIndexed { index, plr ->
                pane[GridPoint.fromBukkitChestSlot(index)!!] = StaticElement(drawable(ItemStack.of(Material.PLAYER_HEAD).apply {
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
                })) {
                    if (!SpectatorController.spectators.contains(plr)) return@StaticElement

                    if (SpectatorController.spectators.contains(plr)) {
                        plr.sendMessage(Format.warning("This player can't be spectated right now."))
                        return@StaticElement
                    }

                    player.tp(plr.location)
                    view.close(TreeTumblers.pluginScope)
                }
            }

            (27..35).forEach {
                pane[GridPoint.fromBukkitChestSlot(it)!!] = StaticElement(drawable(ItemStack.of(Material.GRAY_STAINED_GLASS_PANE).apply {
                    editMeta { meta ->
                        meta.isHideTooltip = true
                    }
                }))
            }

            if(page != 1) {
                pane[4, 0] = StaticElement(drawable(ItemStack.of(Material.ARROW).apply {
                    setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, com.noxcrew.noxesium.api.util.Unit.INSTANCE)
                    itemMeta = itemMeta.also {
                        it.itemName(Format.mm("<yellow>Previous Page</yellow>"))
                        it.lore(listOf(Component.text("Page $page", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)))
                    }
                })) {
                    page--
                }
            }

            if(page != maxPage) {
                pane[4,8] = StaticElement(drawable(ItemStack.of(Material.ARROW).apply {
                    setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, com.noxcrew.noxesium.api.util.Unit.INSTANCE)
                    itemMeta = itemMeta.also {
                        it.itemName(Format.mm("<yellow>Next Page</yellow>"))
                        it.lore(listOf(Component.text("Page $page", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)))
                    }
                })) {
                    page++
                }
            }
        }
    }
}