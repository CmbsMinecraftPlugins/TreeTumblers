package xyz.devcmb.tumblers.util

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.PlayerController
import xyz.devcmb.tumblers.data.TumblingPlayer
import kotlin.math.roundToInt

val playerController: PlayerController by lazy {
    ControllerDelegate.getController("playerController") as PlayerController
}

val Player.tumblingPlayer: TumblingPlayer
    get() {
        return playerController.players.find { it.bukkitPlayer == this }!!
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