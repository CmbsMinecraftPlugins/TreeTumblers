package xyz.devcmb.tumblers.commands.organizer

import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.launch
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.DatabaseController
import xyz.devcmb.tumblers.controllers.event.EventController
import xyz.devcmb.tumblers.controllers.player.PlayerController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.Format

@Suppress("unused")
@Permission("tumbling.organizer")
class ScoreCommand {
    @Command("score player view <player>")
    fun playerView(source: CommandSourceStack, player: TumblingPlayer) {
        val sender = source.sender
        sender.sendMessage(Format.info(Format.mm(
            "<player> has <gold>${player.score}</gold> score.",
            Placeholder.component("player", player.formattedName)
        )))
    }

    @Command("score player set <player> <score>")
    fun playerSet(source: CommandSourceStack, player: TumblingPlayer, score: Int) {
        player.score = score
        val sender = source.sender
        sender.sendMessage(Format.success(Format.mm(
            "<player> now has <gold>$score</gold> score!",
            Placeholder.component("player", player.formattedName)
        )))
    }

    @Command("score team view <team>")
    fun teamView(source: CommandSourceStack, team: Team) {
        val sender = source.sender
        sender.sendMessage(Format.info(Format.mm(
            "The <team:${team.name}:name> have <gold>${team.score}</gold> score.",
        )))
    }

    @Command("score team set <team> <score>")
    fun teamSet(source: CommandSourceStack, team: Team, score: Int, @Flag("distribute") distribute: Boolean) {
        val currentScore = team.score
        team.score = score

        val sender = source.sender
        sender.sendMessage(Format.success(Format.mm(
            "The <team:${team.name}:name> now have <gold>${score}</gold> score!"
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

    @Command("score nuke")
    fun nukeScores(source: CommandSourceStack, @Flag("confirm") confirm: Boolean) {
        val sender = source.sender
        if(!confirm) {
            sender.sendMessage(Format.warning("This action is destructive! Re-run with the --confirm flag to execute."))
            return
        }

        EventController.teamScores.replaceAll { _, _ -> 0 }
        PlayerController.players.forEach {
            it.score = 0
        }

        TreeTumblers.pluginScope.launch {
            DatabaseController.replicateTeamData(EventController.teamScores)
            PlayerController.players.forEach {
                DatabaseController.replicatePlayerData(it)
            }
        }

        sender.sendMessage(Format.success("Scores have been nuked successfully!"))
    }

    @Command("score randomize")
    fun randomizeScores(source: CommandSourceStack, @Flag("confirm") confirm: Boolean) {
        val sender = source.sender
        if(!confirm) {
            sender.sendMessage(Format.warning("This action is destructive! Re-run with the --confirm flag to execute."))
            return
        }

        PlayerController.players.forEach {
            it.score = (500..8000).random()
        }

        EventController.teamScores.forEach { score ->
            EventController.teamScores[score.key] = PlayerController.players
                .filter { it.team == score.key }
                .sumOf { it.score }
        }

        sender.sendMessage(Format.success("Scores have been randomized successfully!"))
    }

    @Command("score hide")
    fun hideScores(source: CommandSourceStack) {
        val sender = source.sender
        EventController.scoresHidden = true
        sender.sendMessage(Format.success("Scores have been hidden successfully!"))
    }

    @Command("score show")
    fun showScores(source: CommandSourceStack) {
        val sender = source.sender
        EventController.scoresHidden = false
        sender.sendMessage(Format.success("Scores have been shown successfully!"))
    }

    @Command("score replicate")
    fun replicateScores(source: CommandSourceStack) {
        val sender = source.sender
        sender.sendMessage(Format.info("Starting replication job..."))
        TreeTumblers.pluginScope.launch {
            DatabaseController.replicateTeamData(EventController.teamScores)
            PlayerController.players.forEach {
                DatabaseController.replicatePlayerData(it)
            }
            sender.sendMessage(Format.success("Replicated scores successfully!"))
        }
    }
}