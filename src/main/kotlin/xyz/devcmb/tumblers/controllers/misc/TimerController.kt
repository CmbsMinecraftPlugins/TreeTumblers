package xyz.devcmb.tumblers.controllers.misc

import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.ControllerBase
import xyz.devcmb.tumblers.engine.Timer

@Controller(Controller.Priority.MEDIUM)
class TimerController : ControllerBase() {
    var timers: HashMap<String, Timer> = HashMap()
    override fun init() {
    }

    fun register(timer: Timer) {
        timers.put(timer.id, timer)
    }

    fun unregister(timer: Timer) {
        timers.remove(timer.id)
    }
}