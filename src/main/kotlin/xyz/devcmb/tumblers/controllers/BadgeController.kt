package xyz.devcmb.tumblers.controllers

import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.item.AdvancedItemStack

@Controller("badgeController", Controller.Priority.MEDIUM)
class BadgeController : IController {
    val collectionItem = AdvancedItemStack(Material.ECHO_SHARD) {
        name(Format.mm("<yellow>Badge Collection</yellow>"))
        lore(listOf(
            Format.mm("<!i>A collection of all the badges you've</!i>"),
            Format.mm("<!i>collected playing <green>Tree Tumblers!</green></!i>")
        ).map {
            it.color(NamedTextColor.WHITE)
        })
        model(NamespacedKey("tumbling", "hub/collection"))

        droppable(false)
        rightClick {
            // TODO: Open inventory
        }
    }

    override fun init() {
    }

    fun giveCollection(player: Player) {
        player.inventory.addItem(collectionItem.build())
    }

    // TODO: Make sure this gets cleared
}