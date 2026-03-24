package xyz.devcmb.tumblers.controllers.games.sniffercaretaker

import org.bukkit.event.Listener
import xyz.devcmb.tumblers.data.Team

interface Task : Listener {
    val team: Team
    val snifferCaretaker: SnifferCaretakerController

    val feeling: String
    val description: String
    val stars: Int

    fun taskComplete() {
        snifferCaretaker.completeTask(this.team, this)
    }
}