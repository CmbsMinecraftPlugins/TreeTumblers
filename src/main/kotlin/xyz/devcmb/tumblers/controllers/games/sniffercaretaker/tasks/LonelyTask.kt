package xyz.devcmb.tumblers.controllers.games.sniffercaretaker.tasks

import net.kyori.adventure.text.Component
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.TextDisplay
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.SnifferCaretakerController
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.Task
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.unpackCoordinates
import xyz.devcmb.tumblers.util.validateCoordinates

class LonelyTask(
    override val team: Team,
    override val id: String,
    override val snifferCaretaker: SnifferCaretakerController,
    override val stars: Int,
    val item: EntityType?
) : Task {
    override val feeling = "lonely"
    override var display: TextDisplay? = null
    override var count = 10

    override fun getDisplayText(): String {
        return "<font:${UserInterfaceUtility.ICONS}>${team.icon}</font> " +
                    "<color:${team.color.asHexString()}>Sniffer</color> is ${feeling}! Bring a " +
                    "<yellow><lang:${item?.translationKey()}></yellow> to its pen for $count seconds!"
    }

    val penCoordinates = snifferCaretaker.currentMap.data.getList("pen")!!.map { it as List<*>
        it.validateCoordinates()
    }

    val penMin = snifferCaretaker.offsetLocation(penCoordinates[0]!!.unpackCoordinates(snifferCaretaker.currentMap.world), team)
    val penMax = snifferCaretaker.offsetLocation(penCoordinates[1]!!.unpackCoordinates(snifferCaretaker.currentMap.world), team)

    fun checkMob(mob: Entity): Boolean {
        val location = mob.location

        if (location.x < penMin.x || location.x > penMax.x) return false
        if (location.y < penMin.y || location.y > penMax.y) return false
        if (location.z < penMin.z || location.z > penMax.z) return false

        snifferCaretaker.completeTask(this.team, this)

        return true
    }

    val task = object : BukkitRunnable() {
        override fun run() {
            snifferCaretaker.spawnedMobs[team]!!.forEach {
                if (it.type == item) {
                    val result = checkMob(it)
                    if (result) return
                }
            }
        }
    }

    override fun init() {
        task.runTaskTimer(TreeTumblers.plugin, 20, 20)
    }

    override fun destroy() {
        task.cancel()
    }
}