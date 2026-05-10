package xyz.devcmb.tumblers.controllers

import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.item.AdvancedItemStack
import xyz.devcmb.tumblers.util.openHandledInventory
import java.sql.Timestamp
import java.util.Date

@Controller(Controller.Priority.MEDIUM)
class BadgeController : IController {
    val collectionItem = AdvancedItemStack(Material.ECHO_SHARD) {
        name(Format.mm("<yellow>Badge Collection</yellow>"))
        lore(listOf(
            Format.mm("<!i>A collection of all the badges you've</!i>"),
            Format.mm("<!i>collected playing <green>Tree Tumblers!</green></!i>")
        ).map {
            it.color(NamedTextColor.WHITE)
            // doing it.decoration(TextDecoration.ITALIC, false) didn't work for whatever reason
        })
        model(NamespacedKey("tumbling", "hub/collection"))

        droppable(false)
        movable(false)

        rightClick {
            it.openHandledInventory("badgeCollectionInventory")
        }
    }

    override fun init() {
    }

    fun giveCollection(player: Player) {
        player.inventory.addItem(collectionItem.build())
    }

    fun grantBadge(player: TumblingPlayer, badge: Badge) {
        if(player.badges.contains(badge)) return
        player.badges.put(badge, Timestamp(Date().time))
    }

    interface Badge {
        val name: String
        val game: String
        val badgeName: String
        val hint: String
    }
}