package xyz.devcmb.tumblers.ui.scoreboard

import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard

interface HandledScoreboard {
    val id: String
    fun getObjectives(scoreboard: Scoreboard): Set<Objective>
}