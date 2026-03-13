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
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format

@Command(name = "whitelist")
@Permission("tumbling.organizer")
class WhitelistCommand {
    val databaseController: DatabaseController by lazy {
        ControllerDelegate.getController("databaseController") as DatabaseController
    }

    @Execute(name = "add")
    fun executeWhitelistAdd(@Context executor: CommandSender, @Arg name: String, @Arg team: Team, @Flag("--confirm") confirm: Boolean) {
        if(!team.playingTeam && !confirm) {
            executor.sendMessage(
                Format.warning("You entered a team which is not playing in the event. If you wish to proceed anyways, rerun the command with the --confirm flag.")
            )

            return
        }

        if(name.length > 16) {
            executor.sendMessage(Format.error("Player does not exist!"))
            return
        }

        TreeTumblers.pluginScope.launch {
            val profile = Bukkit.createProfile(name)
            profile.complete()

            if (profile.isComplete) {
                if(databaseController.isWhitelisted(profile.id.toString())) {
                    executor.sendMessage(Format.warning("Nothing changed. Player is already whitelisted."))
                    return@launch
                }

                databaseController.whitelistPlayer(profile, team)

                executor.sendMessage(
                    Format.success(
                        Component.text("Whitelisted $name on the ")
                            .append(team.FormattedName)
                            .append(Component.text(" team successfully!"))
                    )
                )
            } else {
                executor.sendMessage(Format.error("Player does not exist!"))
            }
        }
    }

    @Execute(name = "remove")
    fun executeWhitelistRemove(@Context executor: CommandSender, @Arg whitelistedPlayer: DatabaseController.WhitelistedPlayer) {
        val name = whitelistedPlayer.name

        if(name.length > 16) {
            executor.sendMessage(Format.error("Player does not exist!"))
            return
        }

        TreeTumblers.pluginScope.launch {
            val profile = Bukkit.createProfile(name)
            profile.complete()

            if (profile.isComplete) {
                try {
                    if(!databaseController.isWhitelisted(profile.id.toString())) {
                        executor.sendMessage(Format.warning("Nothing changed. Player is not whitelisted."))
                        return@launch
                    }

                    databaseController.unwhitelistPlayer(profile)
                    executor.sendMessage(Format.success("Unwhitelisted $name successfully!"))
                } catch (e: Exception) {
                    executor.sendMessage(Format.error("An error occurred while attempting to un-whitelist!"))
                    DebugUtil.severe("Failed to un-whitelist $name: ${e.message ?: "Unknown Error"}")
                }
            } else {
                executor.sendMessage(Format.error("Player does not exist!"))
            }
        }
    }
}
