package xyz.devcmb.tumblers.ui.actionbar.games

import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.entity.Player
import xyz.devcmb.fui.draw.TextDrawContext
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.games.tower_ascent.TowerAscentController
import xyz.devcmb.tumblers.ui.actionbar.HandledActionBar
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class TowerAscentActionBar(val player: Player) : HandledActionBar {
    override val id: String = "towerAscentActionBar"

    /**
     * Called every tick with a passed in [TextDrawContext]
     *
     * When invoked, the cursor will always be set at the origin position
     */
    override fun draw(ctx: TextDrawContext) {
        val controller = GameController.activeGame as? TowerAscentController ?: return

        ctx.drawAligned(Format.mm("<glyph:hud/tower_ascent/gold_counter>").shadowColor(ShadowColor.shadowColor(0)), TextDrawContext.Alignment.RIGHT)
        ctx.moveCursor(0, 22)
        ctx.drawAligned(Format.mm("${controller.playerGoldCounts[player.tumblingPlayer] ?: 0} "), TextDrawContext.Alignment.RIGHT)

//        ctx.moveCursor(0, 0)
//        val handler = controller.generator.towerHandlers.find { it.team == player.tumblingPlayer.team } ?: return
//        val roomController = handler.currentRoom.roomController
//        if(roomController is ShopRoom) {
//            val itemIndex = roomController.playerCurrentShopItems[player] ?: return
//            val item = roomController.shopItems[itemIndex]
//
//            ctx.drawAligned(Format.mm(
//                "<white><item> <dark_gray>-</dark_gray> <gold>${item.shopItem.price}</white>",
//                Placeholder.component("item", item.entity.itemStack.effectiveName())
//            ), TextDrawContext.Alignment.CENTER)
//        }
    }
}