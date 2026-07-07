package xyz.devcmb.tumblers.ui.inventory.spectate

import com.noxcrew.interfaces.drawable.Drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.Interface
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.view.InterfaceView
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController
import xyz.devcmb.tumblers.controllers.player.SpectatorController
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tp

class CrumbleSpectateMenu : HandledInventory {
    override val id: String = "crumbleSpectateMenu"
    override val inventory: Interface<*, *> = buildChestInterface {
        titleSupplier = {
            UserInterfaceUtility.customInventoryTitle(
                Format.mm("<glyph:container/matchup_spectate_menu>"),
                Component.text("Spectate", NamedTextColor.WHITE)
            )
        }
        rows = 4

        withTransform { pane, view ->
            val crumble = GameController.activeGame as? CrumbleController ?: return@withTransform
            if(crumble.roundIndex == -1) {
                view.player.sendMessage(Format.warning("You cannot spectate until the game has started."))
                view.close(TreeTumblers.pluginScope)
                return@withTransform
            }

            crumble.matchups[crumble.roundIndex].forEachIndexed { index, teams ->
                addMatchupSide(pane, view, teams.first.getAllPlayers(), index, 0)
                addMatchupSide(pane, view, teams.second.getAllPlayers(), index, 5)
            }
        }
    }

    fun addMatchupSide(pane: Pane, view: InterfaceView, players: Set<TumblingPlayer>, row: Int, columnOffset: Int) {
        val crumble = GameController.activeGame as? CrumbleController ?: return
        players.take(4).forEachIndexed { plrIndex, player ->
            val model =
                if(player !in crumble.alivePlayers[player.team]!!) NamespacedKey(TreeTumblers.NAMESPACE, "icon/skull")
                else NamespacedKey(TreeTumblers.NAMESPACE, "icon/team/${player.team.name.lowercase()}")

            val item = ItemStack.of(Material.ECHO_SHARD).apply {
                editMeta {
                    it.itemName(player.formattedName)
                    it.lore(listOf(player.team.formattedName).map { entry -> entry.decoration(TextDecoration.ITALIC, false) })
                    it.itemModel = model
                }
            }

            pane[row, plrIndex + columnOffset] = StaticElement(Drawable.drawable(item)) {
                if (!SpectatorController.spectators.contains(it.player)) return@StaticElement

                if (player.bukkitPlayer == null || SpectatorController.spectators.contains(player.bukkitPlayer)) {
                    it.player.sendMessage(Format.warning("This player can't be spectated right now."))
                    return@StaticElement
                }

                it.player.tp(player.bukkitPlayer!!.location)
                view.close(TreeTumblers.pluginScope)
            }
        }
    }
}