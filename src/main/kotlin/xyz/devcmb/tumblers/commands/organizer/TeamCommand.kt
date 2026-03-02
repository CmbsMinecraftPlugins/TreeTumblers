package xyz.devcmb.tumblers.commands.organizer

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.flag.Flag
import dev.rollczi.litecommands.annotations.permission.Permission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.DatabaseController
import xyz.devcmb.tumblers.data.Team

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
                Component.text("You entered a team which is not playing in the event. If you wish to proceed anyways, rerun the command with the --confirm flag.")
                    .color(NamedTextColor.YELLOW)
            )

            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(TreeTumblers.plugin, Runnable {
            val profile = Bukkit.createProfile(name)
            profile.complete()

            if (profile.isComplete) {
                val databaseController = ControllerDelegate.getController("databaseController") as DatabaseController
                databaseController.setPlayerTeam(profile, team, {
                    executor.sendMessage(
                        Component.text("Assigned $name to the ")
                            .append(team.FormattedName)
                            .append(Component.text(" team successfully!"))
                            .color(NamedTextColor.GREEN)
                    )

                    val onlinePlayer = Bukkit.getPlayer(name)
                    if(onlinePlayer?.isOnline == true) {
                        // anti-edge-case-o-matic-9000
                        onlinePlayer.kick(Component.text("Your team has been changed and requires you to rejoin"))
                    }
                }, {
                    executor.sendMessage(Component.text("An error occurred trying to set the team of $name", NamedTextColor.RED))
                })
            } else {
                executor.sendMessage(Component.text("Player does not exist!", NamedTextColor.RED))
            }
        })
    }
}
