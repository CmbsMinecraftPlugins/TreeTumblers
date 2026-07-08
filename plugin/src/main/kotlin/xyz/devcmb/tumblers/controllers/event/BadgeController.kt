package xyz.devcmb.tumblers.controllers.event

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.IController
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.Font
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.item.advanced.AdvancedItemStack
import xyz.devcmb.tumblers.util.openHandledInventory
import xyz.devcmb.tumblers.util.wrapComponent
import java.sql.Timestamp
import java.util.Date

@Controller(Controller.Priority.MEDIUM)
object BadgeController : IController {
    val collectionItem = AdvancedItemStack(Material.ECHO_SHARD) {
        name(Format.mm("<yellow>Badge Collection</yellow>"))
        lore(
            listOf(
                Format.mm("A collection of all the badges you've"),
                Format.mm("collected playing <green>Tree Tumblers!</green>")
            ).map {
                it.color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
            }
        )
        model(NamespacedKey(TreeTumblers.NAMESPACE, "icon/collection"))

        droppable(false)
        movable(false)

        click {
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

        if(player.bukkitPlayer != null) {
            player.bukkitPlayer!!.sendMessage(Format.mm(
                "<green>(<white><icon></white>) You've unlocked a new badge: <gold><hover:show_text:'" +
                        "<gold>${badge.badgeName}</gold><br><white><hint></white>" +
                        "'>[${badge.badgeName}]</hover></gold></green>",
                Placeholder.component("icon", Font.getGlyph("icon/collection")),
                Placeholder.component("hint", wrapComponent(Component.text(badge.hint), 30)
                    .reduce { acc, component -> acc.append(Component.newline()).append(component) }
                )
            ))
        }

        player.badges[badge] = Timestamp(Date().time)
    }

    interface Badge {
        val name: String
        val game: String
        val badgeName: String
        val hint: String
    }
}