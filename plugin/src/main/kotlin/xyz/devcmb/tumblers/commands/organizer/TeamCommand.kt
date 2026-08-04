package xyz.devcmb.tumblers.commands.organizer

import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.Format

@Suppress("unused")
@Permission("tumbling.organizer")
class TeamCommand {
    @Command("team set <player> <team>")
    fun executeTeamSet(
        source: CommandSourceStack,
        player: TumblingPlayer,
        team: Team,
        @Flag("confirm") confirm: Boolean
    ) {
        val executor = source.sender
        if(GameController.activeGame != null) {
            executor.sendMessage(Format.error("You cannot change teams while a game is active!"))
            return
        }

        if(!team.playingTeam && !confirm) {
            executor.sendMessage(
                Format.warning("You entered a team which is not playing in the event. If you wish to proceed anyways, rerun the command with the --confirm flag.")
            )

            return
        }

        player.team = team
        executor.sendMessage(Format.success(Format.mm("Assigned <player:${player.uuid}> to the <team:${team.name}:name> team successfully!")))
        player.bukkitPlayer?.kick(Format.mm("You've been changed to the <color:${team.color.asHexString()}>${team.name.lowercase()}</color> team and need to rejoin."))
    }

    @Command("team list")
    fun executeList(source: CommandSourceStack) {
        val sender = source.sender
        var teams = Component.empty()
        Team.entries.forEach {
            teams = teams.appendNewline().append(it.formattedName)
        }
        sender.sendMessage(Component.text("Here are all the teams: ", NamedTextColor.AQUA).append(teams))
    }
}
