package xyz.devcmb.tumblers.util

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.PlayerController
import xyz.devcmb.tumblers.data.TumblingPlayer

val Player.tumblingPlayer: TumblingPlayer?
    get() {
        val playerController = ControllerDelegate.getController("playerController") as PlayerController
        return playerController.players.find { it.bukkitPlayer == this }
    }

fun runTask(runnable: Runnable) =
    Bukkit.getScheduler().runTask(TreeTumblers.plugin, runnable)
fun runTaskLater(runnable: Runnable, delay: Long) =
    Bukkit.getScheduler().runTaskLater(TreeTumblers.plugin, runnable, delay)
fun runTaskTimer(runnable: Runnable, delay: Long, period: Long) =
    Bukkit.getScheduler().runTaskTimer(TreeTumblers.plugin, runnable, delay, period)
fun runTaskAsynchronously(runnable: Runnable) =
    Bukkit.getScheduler().runTaskAsynchronously(TreeTumblers.plugin, runnable)