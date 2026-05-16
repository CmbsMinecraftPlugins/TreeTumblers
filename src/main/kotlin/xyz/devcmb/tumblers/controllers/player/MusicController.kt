package xyz.devcmb.tumblers.controllers.player

import org.bukkit.Bukkit
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.ControllerBase

@Controller(Controller.Priority.MEDIUM)
class MusicController : ControllerBase() {
    var currentMusic: Music? = null
    val loopingTasks: HashMap<Player, BukkitRunnable> = hashMapOf()

    override fun init() {
    }

    fun playMusic(music: Music) {
        if (currentMusic != null) return
        currentMusic = music

        Bukkit.getOnlinePlayers().forEach {
            val loopTask: BukkitRunnable = object : BukkitRunnable() {
                override fun run() {
                    it.playSound(it.location, music.key, SoundCategory.RECORDS, 1.0f, 1.0f)
                }
            }

            loopingTasks[it] = loopTask
            loopTask.runTaskTimer(TreeTumblers.plugin, 0L, music.duration)
        }
    }

    fun stopMusic() {
        if (currentMusic == null) return
        Bukkit.getOnlinePlayers().forEach {
            it.stopSound(currentMusic!!.key)
            loopingTasks[it]?.cancel()
            loopingTasks.remove(it)
        }

        currentMusic = null
    }

    enum class Music(val key: String, val duration: Long) {
        VOTING("${TreeTumblers.NAMESPACE}:music.voting", 738L)
    }
}