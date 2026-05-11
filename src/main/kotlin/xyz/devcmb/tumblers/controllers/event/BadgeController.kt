package xyz.devcmb.tumblers.controllers.event

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.ControllerBase
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.item.AdvancedItemStack
import xyz.devcmb.tumblers.util.openHandledInventory
import java.sql.Timestamp
import java.util.Date

@Controller(Controller.Priority.MEDIUM)
class BadgeController : ControllerBase() {
    val collectionItem = AdvancedItemStack(Material.ECHO_SHARD) {
        name(Format.mm("<yellow>Badge Collection</yellow>"))
        lore(
            listOf(
                Format.mm("A collection of all the badges you've"),
                Format.mm("collected playing <green>Tree Tumblers!</green>")
            ).map {
                it.color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
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