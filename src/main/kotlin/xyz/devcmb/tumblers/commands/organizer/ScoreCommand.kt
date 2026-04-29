package xyz.devcmb.tumblers.commands.organizer

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.flag.Flag
import dev.rollczi.litecommands.annotations.permission.Permission
import kotlinx.coroutines.launch
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.DatabaseController
import xyz.devcmb.tumblers.controllers.EventController
import xyz.devcmb.tumblers.controllers.PlayerController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.Format

@Command(name = "score")
@Permission("tumbling.organizer")
class ScoreCommand {
    val eventController: EventController by lazy {
        ControllerDelegate.getController<EventController>()
    }

    val playerController: PlayerController by lazy {
        ControllerDelegate.getController<PlayerController>()
    }

    val databaseController: DatabaseController by lazy {
        ControllerDelegate.getController<DatabaseController>()
    }

    @Execute(name = "player view")
    fun playerView(@Context sender: CommandSender, @Arg("player") player: TumblingPlayer) {
        sender.sendMessage(Format.info(Format.mm(
            "<player> has <gold>${player.score}</gold> score.",
            Placeholder.component("player", player.formattedName)
        )))
    }

    @Execute(name = "player set")
    fun playerSet(@Context sender: CommandSender, @Arg("player") player: TumblingPlayer, @Arg("score") score: Int) {
        player.score = score
        sender.sendMessage(Format.success(Format.mm(
            "<player> now has <gold>$score</gold> score!",
            Placeholder.component("player", player.formattedName)
        )))
    }

    @Execute(name = "team view")
    fun teamView(@Context sender: CommandSender, @Arg("team") team: Team) {
        sender.sendMessage(Format.info(Format.mm(
            "The <team> have <gold>${team.score}</gold> score.",
            Placeholder.component("team", team.formattedName)
        )))
    }

    @Execute(name = "team set")
    fun teamSet(@Context sender: CommandSender, @Arg("team") team: Team, @Arg("score") score: Int, @Flag("--distribute") distribute: Boolean) {
        val currentScore = team.score
        team.score = score

        sender.sendMessage(Format.success(Format.mm(
            "The <team> now have <gold>${score}</gold> score!",
            Placeholder.component("team", team.formattedName)
        )))

        if(distribute && score >= currentScore) {
            val players = team.getAllPlayers()
            var remainder = score % players.size
            players.forEach {
                var score = score / players.size
                if(remainder > 0) {
                    score++
                    remainder--
                }

                // maybe make this add?
                it.score = score
            }

            sender.sendMessage(Format.success(Format.mm(
                "<gold>${score / players.size}</gold><remainder> score has been distributed to team players successfully!",
                // just to indicate if the score needed to be distributed unevenly
                // +r = +1 remainder point to some players
                Placeholder.parsed("remainder", (if(score % players.size != 0) " <dark_gray>(+r)</dark_gray>" else ""))
            )))
        }
    }

    @Execute(name = "nuke")
    fun nukeScores(@Context sender: CommandSender, @Flag("--confirm") confirm: Boolean) {
        if(!confirm) {
            sender.sendMessage(Format.warning("This action is destructive! Re-run with the --confirm flag to execute."))
            return
        }

        eventController.teamScores.replaceAll { _, _ -> 0 }
        playerController.players.forEach {
            it.score = 0
        }

        TreeTumblers.pluginScope.launch {
            databaseController.replicateTeamData(eventController.teamScores)
            playerController.players.forEach {
                databaseController.replicatePlayerData(it)
            }
        }

        sender.sendMessage(Format.success("Scores have been nuked successfully!"))
    }

    @Execute(name = "hide")
    fun hideScores(@Context sender: CommandSender) {
        eventController.scoresHidden = true
        sender.sendMessage(Format.success("Scores have been hidden successfully!"))
    }

    @Execute(name = "show")
    fun showScores(@Context sender: CommandSender) {
        eventController.scoresHidden = false
        sender.sendMessage(Format.success("Scores have been shown successfully!"))
    }

    @Execute(name = "replicate")
    fun replicateScores(@Context sender: CommandSender) {
        sender.sendMessage(Format.info("Starting replication job..."))
        TreeTumblers.pluginScope.launch {
            databaseController.replicateTeamData(eventController.teamScores)
            playerController.players.forEach {
                databaseController.replicatePlayerData(it)
            }
            sender.sendMessage(Format.success("Replicated scores successfully!"))
        }
    }
}