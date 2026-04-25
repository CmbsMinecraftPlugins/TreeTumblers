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

    var currentTime = time
    var job: Job? = null
    var endedEarly: Boolean? = null
    var paused: Boolean = false
    var isRunning: Boolean = false

    val timerController by lazy {
        ControllerDelegate.getController("timerController") as TimerController
    }

    // DSL Fields
    var id: String = "timer_${UUID.randomUUID().toString().take(8)}"
    var onComplete: (suspend (interrupted: Boolean) -> Unit)? = null
    var format: () -> Component = format@{
        if(paused) return@format Format.mm("<yellow>PAUSED</yellow>")
        Component.text(MiscUtils.formatToMSS(currentTime))
    }
    var joined: Boolean = false

    val intervalBroadcasts: HashMap<Int, Triple<String, Format.MessageFormatter, ((time: Int) -> Unit)?>> = HashMap()
    val timeBroadcasts: HashMap<Int, Triple<String, Format.MessageFormatter, (() -> Unit)?>> = HashMap()
    val timeExecutions: HashMap<Int, suspend Timer.() -> Unit> = HashMap()

    init {
        this.init()
    }

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
    fun intervalBroadcast(interval: Int, message: String, formatter: Format.MessageFormatter = Format.MessageFormatter.DEFAULT, onBroadcast: ((time: Int) -> Unit)? = null) {
        intervalBroadcasts[interval] = Triple(message, formatter, onBroadcast)
    }

    /**
     * Use the following MiniMessage tags to get timer info
     *
     * minutes - The minutes remaining
     *
     * seconds - The amount of seconds remaining
     *
     * timeSeconds - The amount of seconds in the minute remaining
     *
     * time - The formatted time string (defined by the [format] method)
     */
    fun timeBroadcast(time: Int, message: String, formatter: Format.MessageFormatter = Format.MessageFormatter.DEFAULT, onBroadcast: (() -> Unit)? = null) {
        timeBroadcasts[time] = Triple(message, formatter, onBroadcast)
    }

    fun timeExecution(time: Int, method: suspend Timer.() -> Unit) {
        timeExecutions.put(time, method)
    }


    suspend fun start() {
        require(job == null) { "Timer has already been started." }

        timerController.register(this)
        isRunning = true
        job = TreeTumblers.pluginScope.launch {
            while(true) {
                delay(1000)

                if(paused) continue

                currentTime--

                if(currentTime <= 0) break

                val intervalMessages = intervalBroadcasts.filter { currentTime % it.key == 0 }
                intervalMessages.forEach {
                    it.value.third?.invoke(currentTime)
                    Bukkit.broadcast(it.value.second.formatMessage(Format.mm(
                        it.value.first,
                        Placeholder.unparsed("minutes", (currentTime / 60).toString()),
                        Placeholder.unparsed("seconds", currentTime.toString()),
                        Placeholder.unparsed("timeSeconds", (currentTime % 60).toString()),
                        Placeholder.component("time", format())
                    )))
                }

                if(timeBroadcasts.containsKey(currentTime)) {
                    val broadcastPair = timeBroadcasts[currentTime]!!
                    broadcastPair.third?.invoke()
                    Bukkit.broadcast(broadcastPair.second.formatMessage(Format.mm(
                        broadcastPair.first,
                        Placeholder.unparsed("minutes", (currentTime % 60).toString()),
                        Placeholder.unparsed("seconds", currentTime.toString()),
                        Placeholder.component("time", format())
                    )))
                }

                if(timeExecutions.containsKey(currentTime)) {
                    TreeTumblers.pluginScope.launch {
                        timeExecutions[currentTime]!!.invoke(this@Timer)
                    }
                }
            }

            endedEarly = false
            isRunning = false
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
        isRunning = false
        timerController.unregister(this)
        job = null
    }
}