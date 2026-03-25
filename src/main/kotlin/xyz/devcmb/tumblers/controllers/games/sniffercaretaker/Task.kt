package xyz.devcmb.tumblers.controllers.games.sniffercaretaker

import org.bukkit.entity.TextDisplay
import org.bukkit.event.Listener
import xyz.devcmb.tumblers.data.Team

interface Task : Listener {
    val team: Team
    val snifferCaretaker: SnifferCaretakerController

    val id: String
    val feeling: String
    val description: String
    val stars: Int

    var display: TextDisplay?

    fun setupDisplay() {

    }

    fun taskComplete() {
        snifferCaretaker.completeTask(this.team, this)
    }
}