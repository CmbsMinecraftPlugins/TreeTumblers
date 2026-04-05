package xyz.devcmb.tumblers.controllers.games.sniffercaretaker.tasks

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
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
    override var count = 5

    override fun getDisplayText(): Component {
        return Format.mm(
            "<font:${UserInterfaceUtility.ICONS}>${team.icon}</font> " +
                    "<color:${team.color.asHexString()}>Sniffer</color> is ${feeling}! Bring " +
                    "<sprite:blocks:block/${item?.name?.lowercase()}> " +
                    "$count <yellow><lang:${item?.blockTranslationKey}>${if (count == 1) "" else "s"}</yellow> to its pen!"
        )
    }

    val penCoordinates = snifferCaretaker.currentMap.data.getList("pen")!!.map { it as List<*>
        it.validateCoordinates()
    }

    val penMin = snifferCaretaker.offsetLocation(penCoordinates[0]!!.unpackCoordinates(snifferCaretaker.currentMap.world), team)
    val penMax = snifferCaretaker.offsetLocation(penCoordinates[1]!!.unpackCoordinates(snifferCaretaker.currentMap.world), team)

    @EventHandler
    fun blockPlace(event: BlockPlaceEvent) {
        val tumblingPlayer = event.player.tumblingPlayer
        if (tumblingPlayer.team != team) return

        val block = event.block
        if (block.type != item) return

        if (block.x < penMin.x || block.x > penMax.x) return
        if (block.y < penMin.y || block.y > penMax.y) return
        if (block.z < penMin.z || block.z > penMax.z) return

        snifferCaretaker.completeTask(this.team, this)

        repeat(6) {
            runTaskLater(10L*it) {
                val sourceId = (0..1000000).random()

                team.getOnlinePlayers().forEach { player ->
                    player.sendBlockDamage(block.location, (it / 8f) + 0.1f, sourceId)
                    snifferCaretaker.currentMap.world.playSound(block.location, block.blockData.soundGroup.placeSound, 0.5f, 0.8f)
                }
            }
        }

        runTaskLater(20*3) {
            snifferCaretaker.currentMap.world.spawnParticle(
                Particle.BLOCK,
                block.location,
                10,
                0.05,
                0.05,
                0.05,
                block.blockData
            )

            snifferCaretaker.currentMap.world.playSound(block.location, block.blockData.soundGroup.breakSound, 1f, 1f)

            block.type = Material.AIR
        }
    }

    @EventHandler
    fun blockBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.type != item) return

        if (block.x < penMin.x || block.x > penMax.x) return
        if (block.y < penMin.y || block.y > penMax.y) return
        if (block.z < penMin.z || block.z > penMax.z) return

        event.isCancelled = true
    }
}