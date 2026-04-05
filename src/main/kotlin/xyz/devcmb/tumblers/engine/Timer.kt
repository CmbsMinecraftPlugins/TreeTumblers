package xyz.devcmb.tumblers.engine

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.TimerController
import xyz.devcmb.tumblers.util.MiscUtils

class Timer(val id: String, time: Int, val onComplete: (early: Boolean) -> Unit = {}) {
    var currentTime = time
    var job: Job? = null
    var endedEarly: Boolean? = null
    var paused: Boolean = false

    val timerController by lazy {
        ControllerDelegate.getController("timerController") as TimerController
    }

    fun start() {
        require(job == null) { "Timer has already been started." }

        timerController.register(this)
        job = TreeTumblers.pluginScope.launch {
            while(true) {
                if(paused) continue

                delay(1000)
                if(currentTime <= 0) break
                currentTime--
            }

            endedEarly = false
            onComplete.invoke(false)
            timerController.unregister(this@Timer)
        }
    }

    fun format(): String {
        if(paused) return "PAUSED"

        return MiscUtils.formatToMSS(currentTime)
    }

    suspend fun join() {
        requireNotNull(job) { "Cannot join to an inactive timer" }
        job!!.join()
    }

    fun end() {
        requireNotNull(job) { "Cannot end an inactive timer" }

        endedEarly = true
        job!!.cancel()
        onComplete.invoke(true)
        timerController.unregister(this)
        job = null
    }
}
