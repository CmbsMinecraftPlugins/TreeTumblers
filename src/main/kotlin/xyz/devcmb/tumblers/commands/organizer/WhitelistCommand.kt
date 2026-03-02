package xyz.devcmb.tumblers.commands.organizer

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.flag.Flag
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.DatabaseController
import xyz.devcmb.tumblers.data.Team

@Command(name = "whitelist")
class WhitelistCommand {

    @Execute(name = "add")
    fun executeWhitelistAdd(@Context executor: CommandSender, @Arg name: String, @Arg team: Team, @Flag("--confirm") confirm: Boolean) {
        if(!team.playingTeam && !confirm) {
            executor.sendMessage(
                Component.text("You entered a team which is not playing in the event. If you wish to proceed anyways, rerun the command with the --confirm flag.")
                    .color(NamedTextColor.YELLOW)
            )

            return
        }

        if(name.length > 16) {
            executor.sendMessage(Component.text("Player does not exist!", NamedTextColor.RED))
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(TreeTumblers.plugin, Runnable {
            val profile = Bukkit.createProfile(name)
            profile.complete()

            if (profile.isComplete) {
                val databaseController = ControllerDelegate.getController("databaseController") as DatabaseController
                if(databaseController.isWhitelisted(profile.id.toString())) {
                    executor.sendMessage(Component.text("Nothing changed. Player is already whitelisted.", NamedTextColor.YELLOW))
                    return@Runnable
                }

                databaseController.whitelistPlayer(profile, team, {
                    executor.sendMessage(
                        Component.text("Whitelisted $name on the ")
                            .append(team.FormattedName)
                            .append(Component.text(" team successfully!"))
                            .color(NamedTextColor.GREEN)
                    )
                }, {
                    executor.sendMessage(Component.text("An error occurred trying to whitelist $name", NamedTextColor.RED))
                })
            } else {
                executor.sendMessage(Component.text("Player does not exist!", NamedTextColor.RED))
            }
        })
    }

    @Execute(name = "remove")
    fun executeWhitelistRemove(@Context executor: CommandSender, @Arg whitelistedPlayer: DatabaseController.WhitelistedPlayer) {
        val name = whitelistedPlayer.name

        if(name.length > 16) {
            executor.sendMessage(Component.text("Player does not exist!", NamedTextColor.RED))
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(TreeTumblers.plugin, Runnable {
            val profile = Bukkit.createProfile(name)
            profile.complete()

            if (profile.isComplete) {
                val databaseController = ControllerDelegate.getController("databaseController") as DatabaseController
                if(!databaseController.isWhitelisted(profile.id.toString())) {
                    executor.sendMessage(Component.text("Nothing changed. Player is not whitelisted.", NamedTextColor.YELLOW))
                    return@Runnable
                }

                databaseController.unwhitelistPlayer(profile, {
                    executor.sendMessage(
                        Component.text("Unwhitelisted $name successfully!", NamedTextColor.GREEN)
                    )
                }, {
                    executor.sendMessage(Component.text("An error occurred trying to remove player $name from the whitelist", NamedTextColor.RED))
                })
            } else {
                executor.sendMessage(Component.text("Player does not exist!", NamedTextColor.RED))
            }
        })
    }
}
