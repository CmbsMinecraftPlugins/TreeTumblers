package xyz.devcmb.tumblers.controllers.games.sniffercaretaker

import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.Listener
import xyz.devcmb.tumblers.data.Team

interface Task : Listener {
    val team: Team
    val snifferCaretaker: SnifferCaretakerController

    val id: String
    val feeling: String
    val stars: Int
    var count: Int

    var display: TextDisplay?
    var displayText: String

    var completer: Player?

    fun init() {

    }

    fun destroy() {

    }
}