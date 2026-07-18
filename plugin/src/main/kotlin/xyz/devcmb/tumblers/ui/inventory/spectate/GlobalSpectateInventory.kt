package xyz.devcmb.tumblers.ui.inventory.spectate

import com.noxcrew.interfaces.drawable.Drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.noxesium.api.util.Unit
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

class GlobalSpectateInventory : HandledInventory {
    override val id: String = "spectateInventory"

    override val inventory = buildChestInterface {
        titleSupplier = { UserInterfaceUtility.centerInventoryTitle(
            Component.text("Spectate", NamedTextColor.WHITE)
        ) }
        rows = 5

        withTransform { pane, view ->
            val player = view.player
            val players: List<Player> =
                GameController.activeGame
                    ?.gameParticipants
                    ?.filter { it != player.tumblingPlayer }
                    ?.mapNotNull { it.bukkitPlayer }
                ?: Team.entries
                    .filter { it.playingTeam }
                    .flatMap { it.getOnlinePlayers() }
                    .filter { it != player && it !in SpectatorController.spectators }

            players.forEachIndexed { index, plr ->
                pane[GridPoint.fromBukkitChestSlot(index)!!] =
                    StaticElement(Drawable.drawable(ItemStack.of(Material.PLAYER_HEAD).apply {
                        setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, Unit.INSTANCE)
                        editMeta(SkullMeta::class.java) {
                            it.displayName(Format.formatPlayerName(plr).decoration(TextDecoration.ITALIC, false))
                            it.itemModel = UserInterfaceUtility.FLAT_SKULL
                            it.owningPlayer = plr

                            it.lore(
                                listOf(
                                    plr.tumblingPlayer.team.formattedName
                                ).map { line -> line.decoration(TextDecoration.ITALIC, false) }
                            )
                        }
                    })) {
                        if (!SpectatorController.spectators.contains(player)) return@StaticElement

                        if (SpectatorController.spectators.contains(plr)) {
                            plr.sendMessage(Format.warning("This player can't be spectated right now."))
                            return@StaticElement
                        }

                        player.tp(plr.location)
                        view.close(TreeTumblers.pluginScope)
                    }
            }
        }
    }
}