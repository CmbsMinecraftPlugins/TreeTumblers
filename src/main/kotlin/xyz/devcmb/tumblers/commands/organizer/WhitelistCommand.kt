package xyz.devcmb.tumblers.commands.organizer

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.flag.Flag
import dev.rollczi.litecommands.annotations.permission.Permission
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerRegistry
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.DatabaseController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format

@Command(name = "whitelist")
@Permission("tumbling.organizer")
class WhitelistCommand {
    val databaseController: DatabaseController by ControllerRegistry.controller()

    @Execute(name = "add")
    fun executeWhitelistAdd(@Context executor: CommandSender, @Arg("name") name: String, @Arg("team") team: Team, @Flag("--confirm") confirm: Boolean) {
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
            if (profile.complete(false)) {
                if(databaseController.isWhitelisted(profile.id.toString())) {
                    executor.sendMessage(Format.warning("Nothing changed. Player is already whitelisted."))
                    return@launch
                }

                databaseController.whitelistPlayer(profile, team)

                executor.sendMessage(Format.success(Format.mm("Whitelisted <player:${profile.id}> successfully!")))
            } else {
                executor.sendMessage(Format.error("Player does not exist (or the request failed)!"))
            }
        }
    }

    @Execute(name = "remove")
    fun executeWhitelistRemove(@Context executor: CommandSender, @Arg whitelistedPlayer: TumblingPlayer) {
        val name = whitelistedPlayer.name

        if(name.length > 16) {
            executor.sendMessage(Format.error("Player does not exist!"))
            return
        }

        TreeTumblers.pluginScope.launch {
            val profile = Bukkit.createProfile(name)
            if (profile.complete(false)) {
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
                executor.sendMessage(Format.error("Player does not exist (or the request failed)!"))
            }
        }
    }
}
