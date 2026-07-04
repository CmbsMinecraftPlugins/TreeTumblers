package xyz.devcmb.tumblers.ui.inventory.brawl

import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.ChestInterface
import com.noxcrew.interfaces.interfaces.buildChestInterface
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.TumblingUIException
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.brawl.BrawlController
import xyz.devcmb.tumblers.controllers.games.brawl.BrawlKit
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.Font
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.buttonClickSound
import xyz.devcmb.tumblers.util.tumblingPlayer

class BrawlKitSelector : HandledInventory {
    override val id: String = "brawlKitSelector"
    override val inventory: ChestInterface = buildChestInterface {
        titleSupplier = {
            UserInterfaceUtility.customInventoryTitle(
                Font.getGlyph("container/brawl_kit_selector"),
                Component.text("Kit Selector", NamedTextColor.WHITE)
            )
        }
        rows = 4

        withTransform { pane, view ->
            val brawl = GameController.activeGame as? BrawlController
                ?: throw TumblingUIException("Attempted to load BrawlKitSelector while the active game is not brawl.")

            fun selectKit(kit: BrawlKit) {
                val kitPlayers = brawl.getSelectedKitPlayers(view.player.tumblingPlayer.team, kit)
                if(view.player.tumblingPlayer in kitPlayers) {
                    view.player.sendMessage(Format.error("You've already selected this kit!"))
                    return
                }

                if(kitPlayers.size >= 2) {
                    view.player.sendMessage(Format.error("This kit has too many players!"))
                    return
                }

                view.player.buttonClickSound()
                brawl.selectKit(view.player.tumblingPlayer, kit, true)
                UserInterfaceUtility.refreshAll("brawlKitSelector")
            }

            val roundKits = brawl.roundKits[brawl.roundIndex]
            roundKits.forEachIndexed { index, it ->
                pane[index, 3] = StaticElement(drawable(ItemStack(Material.ECHO_SHARD).apply {
                    editMeta { meta ->
                        meta.itemName(Format.mm("<white>${it.kitName}</white>"))
                        meta.itemModel = NamespacedKey(TreeTumblers.NAMESPACE, "icon/brawl/${it.name.lowercase()}")
                    }
                })) { _ -> selectKit(it) }
            }

            repeat(4) { kitIndex ->
                val kit = roundKits.getOrNull(kitIndex) ?: return@repeat
                val kitPlayers = brawl.getSelectedKitPlayers(view.player.tumblingPlayer.team, kit)

                repeat(2) {
                    val plr = kitPlayers.getOrNull(it)
                    val item = if(plr == null) {
                        ItemStack(Material.ECHO_SHARD).apply {
                            editMeta { meta ->
                                meta.itemName(Format.mm("<green>Select this kit!</green>"))
                                meta.itemModel = NamespacedKey(TreeTumblers.NAMESPACE, "empty")
                            }
                        }
                    } else {
                        ItemStack(Material.PLAYER_HEAD).apply {
                            editMeta(SkullMeta::class.java) { meta ->
                                meta.owningPlayer = Bukkit.getOfflinePlayer(plr.uuid)
                                meta.displayName(plr.formattedName.decoration(TextDecoration.ITALIC, false))
                                meta.itemModel = NamespacedKey(TreeTumblers.NAMESPACE, "flat_skull")
                            }
                        }
                    }

                    pane[kitIndex, 4 + it] = StaticElement(drawable(item)) {
                        selectKit(kit)
                    }
                }
            }
        }
    }
}