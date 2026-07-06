package xyz.devcmb.tumblers.commands.organizer

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.flag.Flag
import dev.rollczi.litecommands.annotations.permission.Permission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.Format

@Command(name = "team")
@Permission("tumbling.organizer")
class TeamCommand {
    @Execute(name = "set")
    fun executeTeamSet(
        @Context executor: CommandSender,
        @Arg("player") player: TumblingPlayer,
        @Arg("team") team: Team,
        @Flag("--confirm") confirm: Boolean
    ) {
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

    @Execute(name = "list")
    fun executeList(@Context sender: CommandSender) {
        var teams = Component.empty()
        Team.entries.forEach { it ->
            teams = teams.appendNewline().append(it.formattedName)
        }
        sender.sendMessage(Component.text("Here are all the teams: ", NamedTextColor.AQUA).append(teams))
    }
}
