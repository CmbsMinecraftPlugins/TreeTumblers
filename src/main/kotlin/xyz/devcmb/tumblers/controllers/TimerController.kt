package xyz.devcmb.tumblers.controllers

import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.engine.Timer

@Controller("timerController")
class TimerController : IController {
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