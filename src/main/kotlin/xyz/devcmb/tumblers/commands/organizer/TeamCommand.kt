package xyz.devcmb.tumblers.commands.organizer

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.flag.Flag
import dev.rollczi.litecommands.annotations.permission.Permission
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.DatabaseController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync

@Command(name = "team")
@Permission("tumbling.organizer")
class TeamCommand {

    @Execute(name = "set")
    fun executeTeamSet(
        @Context executor: CommandSender,
        @Arg whitelistedPlayer: DatabaseController.WhitelistedPlayer,
        @Arg team: Team,
        @Flag("--confirm") confirm: Boolean
    ) {
        val name = whitelistedPlayer.name
        if(!team.playingTeam && !confirm) {
            executor.sendMessage(
                Format.warning("You entered a team which is not playing in the event. If you wish to proceed anyways, rerun the command with the --confirm flag.")
            )

            return
        }

        TreeTumblers.pluginScope.launch {
            val profile = Bukkit.createProfile(name)
            profile.complete()

            if (profile.isComplete) {
                val databaseController = ControllerDelegate.getController("databaseController") as DatabaseController
                databaseController.setPlayerTeam(profile, team)

                executor.sendMessage(
                    Format.success(
                        Component.text("Assigned $name to the ")
                            .append(team.formattedName)
                            .append(Component.text(" team successfully!"))
                    )
                )

                val onlinePlayer = Bukkit.getPlayer(name)
                if(onlinePlayer?.isOnline == true) {
                    // anti-edge-case-o-matic-9000
                    suspendSync {
                        onlinePlayer.kick(Component.text("Your team has been changed and requires you to rejoin"))
                    }
                }
            } else {
                executor.sendMessage(Format.error("Player does not exist!"))
            }
        }
    }
}
