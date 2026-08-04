package xyz.devcmb.tumblers.commands.organizer

import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.DatabaseController
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format

@Suppress("unused")
@Permission("tumbling.organizer")
class WhitelistCommand {
    @Command("whitelist add <name> <team>")
    fun executeWhitelistAdd(source: CommandSourceStack, name: String, team: Team, @Flag("confirm") confirm: Boolean) {
        val executor = source.sender
        if(GameController.activeGame != null) {
            executor.sendMessage(Format.error("Cannot add to whitelist while a game is active."))
            return
        }

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
                if(DatabaseController.isWhitelisted(profile.id.toString())) {
                    executor.sendMessage(Format.warning("Nothing changed. Player is already whitelisted."))
                    return@launch
                }

                DatabaseController.whitelistPlayer(profile, team)

                executor.sendMessage(Format.success(Format.mm("Whitelisted <player:${profile.id}> successfully!")))
            } else {
                executor.sendMessage(Format.error("Player does not exist (or the request failed)!"))
            }
        }
    }

    @Command("whitelist remove <player>")
    fun executeWhitelistRemove(source: CommandSourceStack, player: TumblingPlayer) {
        val executor = source.sender
        if(GameController.activeGame != null) {
            executor.sendMessage(Format.error("Cannot remove from whitelist while a game is active."))
            return
        }

        TreeTumblers.pluginScope.launch {
            try {
                if(!DatabaseController.isWhitelisted(player.uuid.toString())) {
                    executor.sendMessage(Format.warning("Nothing changed. Player is not whitelisted."))
                    return@launch
                }

                DatabaseController.unwhitelistPlayer(player)
                executor.sendMessage(Format.success("Unwhitelisted ${player.name} successfully!"))
            } catch (e: Exception) {
                executor.sendMessage(Format.error("An error occurred while attempting to un-whitelist!"))
                DebugUtil.severe("Failed to un-whitelist ${player.name}: ${e.message ?: "Unknown Error"}")
            }
        }
    }
}
