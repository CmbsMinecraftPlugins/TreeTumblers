package xyz.devcmb.tumblers.ui.scoreboard

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.RenderType
import org.bukkit.scoreboard.Score
import org.bukkit.scoreboard.Scoreboard
import xyz.devcmb.tumblers.ControllerRegistry
import xyz.devcmb.tumblers.controllers.event.EventController
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.addScoreboardObjectiveLines

sealed interface HandledScoreboard {
    val id: String
    val displayName: String

    fun enable(scoreboard: Scoreboard)
    fun update(scoreboard: Scoreboard)
    fun disable(scoreboard: Scoreboard)

    abstract class SidebarScoreboard : HandledScoreboard {
        var objective: Objective? = null
        val lastLines: ArrayList<Component> = ArrayList()
        val scores: ArrayList<Score> = ArrayList()

        override fun enable(scoreboard: Scoreboard) {
            objective = scoreboard.registerNewObjective(
                id,
                Criteria.DUMMY,
                Format.mm(
                    displayName,
                    Placeholder.unparsed("game", EventController.game.toString()),
                    Placeholder.unparsed("total", EventController.totalGames.toString())
                )
            )

            objective!!.displaySlot = DisplaySlot.SIDEBAR

            updateScores()
        }

        override fun update(scoreboard: Scoreboard) {
            val lines = getLines()
            if(lastLines == lines) return

            scores.forEach {
                it.resetScore()
            }
            scores.clear()

            updateScores()
        }

        override fun disable(scoreboard: Scoreboard) {
            scores.clear()
            objective?.unregister()
            objective = null
        }

        abstract fun getLines(): ArrayList<Component>

        private fun updateScores() {
            if(objective == null) return

            val lines = getLines()
            lastLines.clear()
            lastLines.addAll(lines)

            val scores = addScoreboardObjectiveLines(objective!!, lines)
            this.scores.addAll(scores)
        }
    }

    abstract class StaticScoreboard(
        val criteria: Criteria = Criteria.DUMMY,
        val renderType: RenderType = RenderType.INTEGER,
        val displaySlot: DisplaySlot = DisplaySlot.BELOW_NAME
    ) : HandledScoreboard {
        var objective: Objective? = null

        override fun enable(scoreboard: Scoreboard) {
            objective = scoreboard.registerNewObjective(
                id,
                criteria,
                Format.mm(displayName),
                renderType
            )
            objective!!.displaySlot = displaySlot
        }

        override fun update(scoreboard: Scoreboard) {}

        override fun disable(scoreboard: Scoreboard) {
            objective?.unregister()
            objective = null
        }
    }
}