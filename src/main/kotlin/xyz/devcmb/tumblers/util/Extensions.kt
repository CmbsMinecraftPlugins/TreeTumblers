package xyz.devcmb.tumblers.util

import com.sk89q.worldedit.math.BlockVector3
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.PlayerController
import xyz.devcmb.tumblers.data.TumblingPlayer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val playerController: PlayerController by lazy {
    ControllerDelegate.getController("playerController") as PlayerController
}

val Player.tumblingPlayer: TumblingPlayer
    get() {
        return playerController.players.find { it.uuid == this.uniqueId }!!
    }

val Player.formattedName: Component
    get() {
        return Format.formatPlayerName(this.tumblingPlayer)
    }

fun Player.openHandledInventory(id: String) {
    playerController.playerUIControllers[this]!!.openInventory(id)
}

fun Player.enableBossBar(id: String) {
    playerController.playerUIControllers[this]!!.enableBossBar(id)
}

fun Player.disableBossBar(id: String) {
    playerController.playerUIControllers[this]!!.disableBossBar(id)
}

fun Player.activateScoreboard(id: String) {
    DebugUtil.info("Activating score board $id for $name")
    playerController.playerUIControllers[this]!!.activateScoreboard(id)
}

fun Player.deactivateScoreboard(id: String) {
    DebugUtil.info("Deactivating score board $id for $name")
    playerController.playerUIControllers[this]!!.deactivateScoreboard(id)
}

fun Player.hunger() {
    addPotionEffect(PotionEffect(PotionEffectType.HUNGER, PotionEffect.INFINITE_DURATION, 1, true, false, false))
}

fun Player.giveKit(kit: Kit.KitDefinition) {
    Kit.giveKit(this, kit)
}

fun List<Location>.getPlayers(heightUp: Int, heightDown: Int, condition: ((player: Player) -> Boolean)? = null): List<Player> {
    return Bukkit.getOnlinePlayers().filter { condition?.invoke(it) ?: true }.filter { player ->
        val playerLocation = player.location

        any { location ->
            location.world == playerLocation.world &&
                playerLocation.blockX == location.blockX &&
                playerLocation.blockZ == location.blockZ &&
                playerLocation.blockY in (location.blockY - heightDown)..(location.blockY + heightUp)
        }
    }
}

fun runTask(runnable: Runnable) =
    Bukkit.getScheduler().runTask(TreeTumblers.plugin, runnable)
fun runTaskLater(delay: Long, runnable: Runnable) =
    Bukkit.getScheduler().runTaskLater(TreeTumblers.plugin, runnable, delay)
fun runTaskTimer(delay: Long, period: Long, runnable: Runnable) =
    Bukkit.getScheduler().runTaskTimer(TreeTumblers.plugin, runnable, delay, period)
fun runTaskAsynchronously(runnable: Runnable) =
    Bukkit.getScheduler().runTaskAsynchronously(TreeTumblers.plugin, runnable)

fun List<Double>.unpackCoordinates(world: World): Location {
    return Location(
        world,
        this[0],
        this[1],
        this[2],
        getOrNull(3)?.toFloat() ?: 0f,
        getOrNull(4)?.toFloat() ?: 0f
    )
}

// thanks ai
// what does any of this mean
inline fun <reified T> List<*>.validateList(): List<T>? {
    if (!all { it is T }) return null
    return map { it as T }
}

fun List<*>.validateLocation(world: World): Location? {
    val list = this.map {
        if(it !is Double && it !is Int && it !is Float) return@validateLocation null
        it.toDouble()
    }

    return list.unpackCoordinates(world)
}

fun Location.isInRegion(bound1: Location, bound2: Location): Boolean {
    return this.blockX >= min(bound1.blockX, bound2.blockX) && this.blockY >= min(bound1.blockY, bound2.blockY)
        && this.blockZ >= min(bound1.blockZ, bound2.blockZ) && this.blockX <= max(bound1.blockX, bound2.blockX)
        && this.blockY <= max(bound1.blockY, bound2.blockY) && this.blockZ <= max(bound1.blockY, bound2.blockY)
}

fun Location.forEachRegion(other: Location, execute: (block: Block) -> Unit) {
    for(x in min(this.x, other.x).toInt()..max(this.x, other.x).toInt())
    for(y in min(this.y, other.y).toInt()..max(this.y, other.y).toInt())
    for(z in min(this.z, other.z).toInt()..max(this.z, other.z).toInt()) {
        execute(this.world.getBlockAt(x, y, z))
    }
}

fun Location.toBlockVector3(): BlockVector3 {
    return BlockVector3.at(this.x, this.y, this.z)
}

fun Location.randomBetween(other: Location): Location {
    val x = (min(this.x.toInt(), other.x.toInt())..max(this.x.toInt(), other.x.toInt())).random()
    val y = (min(this.y.toInt(), other.y.toInt())..max(this.y.toInt(), other.y.toInt())).random()
    val z = (min(this.z.toInt(), other.z.toInt())..max(this.z.toInt(), other.z.toInt())).random()

    return Location(this.world, x.toDouble(), y.toDouble(), z.toDouble())
}

fun Location.minOf(other: Location): Location {
    return Location(this.world, min(this.x, other.x), min(this.y, other.y), min(this.z, other.z))
}

fun Location.maxOf(other: Location): Location {
    return Location(this.world, max(this.x, other.x), max(this.y, other.y), max(this.z, other.z))
}

fun World.fill(location1: Location, location2: Location, material: Material) {
    val xRange = (min(location1.x.toInt(), location2.x.toInt())..max(location1.x.toInt(), location2.x.toInt()))
    val yRange = (min(location1.y.toInt(), location2.y.toInt())..max(location1.y.toInt(), location2.y.toInt()))
    val zRange = (min(location1.z.toInt(), location2.z.toInt())..max(location1.z.toInt(), location2.z.toInt()))

    xRange.forEach { x ->
        yRange.forEach { y ->
            zRange.forEach { z ->
                this.getBlockAt(x, y, z).type = material
            }
        }
    }
}

val Long.tickSeconds: Double
    get() {
        return ((this / 20.0) * 10.0).roundToInt() / 10.0
    }

fun Player.buttonClickSound() {
    player!!.playSound(player!!.location, Sound.UI_BUTTON_CLICK, 10f, 1f)
}

fun Player.sound(sound: Sound) {
    player!!.playSound(player!!.location, sound, 10f, 1f)
}

fun Player.hideToAll() {
    playerController.hiddenPlayers.add(this)
    Bukkit.getOnlinePlayers().forEach {
        if(it !== this) {
            it.hidePlayer(TreeTumblers.plugin, this)
        }
    }
}

fun Player.showToAll() {
    playerController.hiddenPlayers.remove(this)
    Bukkit.getOnlinePlayers().forEach {
        if(it !== this) {
            it.showPlayer(TreeTumblers.plugin, this)
        }
    }
}