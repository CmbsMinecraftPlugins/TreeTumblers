package xyz.devcmb.tumblers.ui.inventory.breach

import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.ChestInterface
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.noxesium.api.util.Unit
import com.noxcrew.noxesium.core.registry.CommonItemComponentTypes
import com.noxcrew.noxesium.paper.component.setNoxesiumComponent
import kotlinx.coroutines.launch
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.TumblingUIException
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.breach.BreachController
import xyz.devcmb.tumblers.controllers.games.breach.BreachKit
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.buttonClickSound
import xyz.devcmb.tumblers.util.tumblingPlayer

object BreachKitSelector : HandledInventory {
    override val id: String = "breachKitSelector"

    override val inventory: ChestInterface = buildChestInterface {
        titleSupplier = { Format.mm("<white>Pick your Weapon</white>") }
        rows = 3

        withTransform { pane, view ->
            val breach = GameController.activeGame as? BreachController ?:
                throw TumblingUIException("Attempted to load BreachKitSelector while the active game is not breach.")

            val player = view.player

            BreachKit.entries.forEachIndexed { index, kit ->
                val item = ItemStack.of(kit.item).apply {
                    setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, Unit.INSTANCE)
                    editMeta {
                        it.itemName(kit.label)
                        it.lore(kit.description.map { entry -> entry.decoration(TextDecoration.ITALIC, false) })
                    }
                }

                pane[1, (index * 2) + 2] = StaticElement(drawable(item)) {
                    breach.giveKit(it.player, kit)
                    it.player.buttonClickSound()
                    TreeTumblers.pluginScope.launch {
                        it.view.reopen()
                    }
                }
            }

            val team = player.tumblingPlayer.team
            var itemHolder: Player? = when (team) {
                breach.playingTeams.first -> breach.team1holder
                breach.playingTeams.second -> breach.team2holder
                else -> null
            }

            val star = ItemStack.of(Material.NETHER_STAR).apply {
                setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, Unit.INSTANCE)
                itemMeta = itemMeta.also { meta ->
                    if (itemHolder == null) {
                        meta.itemName(Format.mm("<light_purple>Hold the Star"))
                        meta.lore(
                            listOf(
                                Format.mm("<aqua>Keep it safe."),
                                Format.mm("<aqua>Your victory depends on it.")
                            )
                        )
                    } else if (itemHolder != player) {
                        meta.itemName(Format.mm("<dark_purple>Star held by ${itemHolder.name}"))
                        meta.lore(
                            listOf(
                                Format.mm("<aqua>Keep ${itemHolder.name} safe."),
                                Format.mm("<aqua>Your victory depends on it.")
                            )
                        )
                    } else if (itemHolder == player) {
                        meta.itemName(Format.mm("<dark_purple>You hold the Star"))
                        meta.lore(
                            listOf(
                                Format.mm("<aqua>Keep it, and yourself safe."),
                                Format.mm("<aqua>Your victory depends on it.")
                            )
                        )
                    }
                }
            }

            pane[2, 4] = StaticElement(drawable(star)) {
                player.buttonClickSound()
                breach.takeItem(player)
                UserInterfaceUtility.refreshAll(id)
            }
        }
    }
}