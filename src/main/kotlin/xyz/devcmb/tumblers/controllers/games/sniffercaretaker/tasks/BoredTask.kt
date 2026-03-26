package xyz.devcmb.tumblers.controllers.games.sniffercaretaker.tasks

import org.bukkit.Material
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockPlaceEvent
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.SnifferCaretakerController
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.Task
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.runTaskLater
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.unpackCoordinates
import xyz.devcmb.tumblers.util.validateCoordinates

class BoredTask(
    override val team: Team,
    override val id: String,
    override val snifferCaretaker: SnifferCaretakerController,
    override val stars: Int,
    val item: Material?
) : Task {
    override val feeling = "bored"
    override var display: TextDisplay? = null

    override val displayText = Format.mm(
        "<font:${UserInterfaceUtility.ICONS}>${team.icon}</font> " +
                "<color:${team.color.asHexString()}>Sniffer</color> is ${feeling}! Bring " +
                "<sprite:blocks:block/${item?.name?.lowercase()}> <yellow><lang:${item?.blockTranslationKey}></yellow> to its pen!")

    @EventHandler
    fun blockPlace(event: BlockPlaceEvent) {
        val tumblingPlayer = event.player.tumblingPlayer
        if (tumblingPlayer.team != team) return

        val block = event.block
        if (block.type != item) return

        val penCoordinates = snifferCaretaker.currentMap.data.getList("pens.${team.name.lowercase()}")!!.map { it as List<*>
            it.validateCoordinates()
        }

        val penMin = penCoordinates[0]!!.unpackCoordinates(snifferCaretaker.currentMap.world)
        val penMax = penCoordinates[1]!!.unpackCoordinates(snifferCaretaker.currentMap.world)

        if (block.x < penMin.x || block.x > penMax.x) return
        if (block.y < penMin.y || block.y > penMax.y) return
        if (block.z < penMin.z || block.z > penMax.z) return

        snifferCaretaker.completeTask(this.team, this)

        runTaskLater(20*3) {
            block.type = Material.AIR
        }
    }
}