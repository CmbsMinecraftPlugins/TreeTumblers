package xyz.devcmb.tumblers.controllers.games.sniffercaretaker.tasks

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.SnifferCaretakerController
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.Task
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.runTaskLater
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.validateLocation

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
    override var completer: Player? = null

    override var displayText = ""
        get() = "<font:${UserInterfaceUtility.ICONS}>${team.icon}</font> " +
                "<color:${team.color.asHexString()}>Sniffer</color> is ${feeling}! Bring " +
                "<sprite:blocks:block/${item?.name?.lowercase()}> " +
                "$count <yellow><lang:${item?.blockTranslationKey}></yellow> to its pen!"


    val penCoordinates = snifferCaretaker.currentMap.data.getList("pen")!!.map { it as List<*>
        it.validateLocation(snifferCaretaker.currentMap.world)
    }

    val penMin = snifferCaretaker.offsetLocation(penCoordinates[0]!!, team)
    val penMax = snifferCaretaker.offsetLocation(penCoordinates[1]!!, team)

    @EventHandler
    fun blockPlace(event: BlockPlaceEvent) {
        val tumblingPlayer = event.player.tumblingPlayer
        if (tumblingPlayer.team != team) return

        val block = event.block
        if (block.type != item) return

        if (block.x < penMin.x || block.x > penMax.x) return
        if (block.y < penMin.y || block.y > penMax.y) return
        if (block.z < penMin.z || block.z > penMax.z) return

        this.completer = event.player

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

        if (
            block.x < penMin.x || block.x > penMax.x ||
            block.y < penMin.y || block.y > penMax.y ||
            block.z < penMin.z || block.z > penMax.z) return

        event.isCancelled = true
    }
}