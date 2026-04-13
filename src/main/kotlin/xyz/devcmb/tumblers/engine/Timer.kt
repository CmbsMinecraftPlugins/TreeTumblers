package xyz.devcmb.tumblers.engine

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.TimerController
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import java.util.UUID

class Timer(val time: Int, val init: Timer.() -> Unit = {}) {
    @Deprecated("For more control, this has been replaced with a DSL. Use that instead.")
    constructor(id: String, time: Int, onComplete: suspend (early: Boolean) -> Unit = {}) : this(time) {
        this.id = id
        this.onComplete = onComplete
    }

    init {
        this.init()
    }

    var currentTime = time
    var job: Job? = null
    var endedEarly: Boolean? = null
    var paused: Boolean = false

    val timerController by lazy {
        ControllerDelegate.getController("timerController") as TimerController
    }

    // DSL Fields
    var id: String = "timer_${UUID.randomUUID().toString().take(8)}"
    var onComplete: (suspend (interrupted: Boolean) -> Unit)? = null
    var format: () -> Component = format@{
        if(paused) return@format Format.mm("<yellow></yellow>")
        Component.text(MiscUtils.formatToMSS(currentTime))
    }
    var joined: Boolean = false

    val intervalBroadcasts: HashMap<Int, String> = hashMapOf()
    val timeBroadcasts: HashMap<Int, String> = hashMapOf()

    fun onComplete(onComplete: (suspend (interrupted: Boolean) -> Unit)? = null) {
        this.onComplete = onComplete
    }

    /**
     * Use the following MiniMessage tags to get timer info
     *
     * minutes - The minutes remaining
     *
     * seconds - The amount of seconds remaining
     *
     * time - The formatted time string (defined by the [format] method)
     */
    fun intervalBroadcast(interval: Int, message: String) {
        intervalBroadcasts[interval] = message
    }

    /**
     * Use the following MiniMessage tags to get timer info
     *
     * minutes - The minutes remaining
     *
     * seconds - The amount of seconds remaining
     *
     * time - The formatted time string (defined by the [format] method)
     */
    fun timeBroadcast(time: Int, message: String) {
        timeBroadcasts[time] = message
    }

    suspend fun start() {
        require(job == null) { "Timer has already been started." }

        timerController.register(this)
        job = TreeTumblers.Companion.pluginScope.launch {
            while(true) {
                if(paused) continue

                delay(1000)
                currentTime--

                if(currentTime <= 0) break

                val intervalMessages = intervalBroadcasts.filter { currentTime % it.key == 0 }
                intervalMessages.forEach {
                    Bukkit.broadcast(Format.mm(
                        it.value,
                        Placeholder.unparsed("minutes", (currentTime % 60).toString()),
                        Placeholder.unparsed("seconds", currentTime.toString()),
                        Placeholder.component("time", format())
                    ))
                }

                if(timeBroadcasts.containsKey(currentTime)) {
                    Bukkit.broadcast(Format.mm(
                        timeBroadcasts[currentTime]!!,
                        Placeholder.unparsed("minutes", (currentTime % 60).toString()),
                        Placeholder.unparsed("seconds", currentTime.toString()),
                        Placeholder.component("time", format())
                    ))
                }
            }

            endedEarly = false
            onComplete?.invoke(false)
            timerController.unregister(this@Timer)
        }
        if(joined) job!!.join()
    }

    suspend fun join() {
        requireNotNull(job) { "Cannot join to an inactive timer" }
        job!!.join()
    }

    suspend fun end() {
        requireNotNull(job) { "Cannot end an inactive timer" }

        endedEarly = true
        job!!.cancel()
        onComplete?.invoke(true)
        timerController.unregister(this)
        job = null
    }
}