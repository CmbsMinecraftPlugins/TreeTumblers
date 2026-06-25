package xyz.devcmb.tumblers.controllers.misc

import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.IController
import xyz.devcmb.tumblers.engine.Timer

@Controller(Controller.Priority.MEDIUM)
object TimerController : IController {
    var timers: HashMap<String, Timer> = HashMap()
    override fun init() {
    }

    fun register(timer: Timer) {
        timers[timer.id] = timer
    }

    fun unregister(timer: Timer) {
        timers.remove(timer.id)
    }
}