package xyz.devcmb.tumblers.controllers

import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.util.tumblingPlayer

@Controller("eventController", Controller.Priority.MEDIUM)
class EventController : IController {
    lateinit var teamScores: HashMap<Team, Int>
    private val databaseController: DatabaseController by lazy {
        ControllerDelegate.getController("databaseController") as DatabaseController
    }

    override fun init() {
        TreeTumblers.pluginScope.launch {
            teamScores = databaseController.getTeamScores()
        }
    }

    fun grantScore(player: Player, amount: Int) {
        val tumblingPlayer = player.tumblingPlayer

        tumblingPlayer.score += amount
        teamScores.put(tumblingPlayer.team, (teamScores[tumblingPlayer.team] ?: 0) + amount)
    }

    fun grantTeamScore(team: Team, amount: Int) {
        teamScores.put(team, (teamScores[team] ?: 0) + amount)
    }

    override fun cleanup() {
        TreeTumblers.pluginScope.launch {
            databaseController.replicateTeamData(teamScores)
        }
    }
}