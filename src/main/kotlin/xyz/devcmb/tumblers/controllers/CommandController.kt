package xyz.devcmb.tumblers.controllers

import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.annotations.Controller
import dev.rollczi.litecommands.LiteCommands
import dev.rollczi.litecommands.adventure.LiteAdventureExtension
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory
import dev.rollczi.litecommands.bukkit.LiteBukkitMessages
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.commands.InvalidUsageHandler
import xyz.devcmb.tumblers.commands.arguments.*
import xyz.devcmb.tumblers.commands.dev.*
import xyz.devcmb.tumblers.commands.event.EventCommand
import xyz.devcmb.tumblers.commands.games.*
import xyz.devcmb.tumblers.commands.misc.ChatCommand
import xyz.devcmb.tumblers.commands.organizer.*
import xyz.devcmb.tumblers.controllers.games.party.PartyController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.engine.DebugToolkit
import xyz.devcmb.tumblers.engine.Timer
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format

@Controller(Controller.Priority.LOWEST)
class CommandController : IController {
    lateinit var liteCommands: LiteCommands<CommandSender>
    override fun init() {
        liteCommands = LiteBukkitFactory.builder("tumblers", TreeTumblers.plugin)
            .commands(
                DebugCommand(),
                WhitelistCommand(),
                TeamCommand(),
                GameCommand(),
                WorldCommand(),
                ScoreCommand(),
                TimerCommand(),
                EventCommand(),
                PartyCommand(),
                SpectateCommand(),
                ChatCommand(),
                NametagCommand(),
                QibCommand(),
            )
            .argument(DebugUtil.DebugLogLevel::class.java, DebugLogLevelArgument())
            .argument(Team::class.java, TeamArgument())
            .argument(DatabaseController.WhitelistedPlayer::class.java, WhitelistedPlayerArgument())
            .argument(GameController.Game::class.java, GameArgument())
            .argument(WorldController.LoadableTemplate::class.java, TemplateWorldArgument())
            .argument(DebugToolkit.DebuggingEvent::class.java, DebuggingEventArgument())
            .argument(Timer::class.java, TimerArgument())
            .argument(PartyController.PartyGameIdentifier::class.java, PartyGameArgument())
            .argument(PartyController.PartyGameSchematic::class.java, PartyGameSchematicArgument())
            .argument(PlayerController.ChatChannel::class.java, ChatChannelArgument())
            .argument(TumblingPlayer::class.java, TumblingPlayerArgument())
            .argument(NoxesiumController.QibType::class.java, QibTypeArgument())
            .extension(LiteAdventureExtension<CommandSender>()) { config ->
                config.serializer(Format.miniMessage)
            }
            .invalidUsage(InvalidUsageHandler())
            .message(LiteBukkitMessages.MISSING_PERMISSIONS, Format.error("You do not have permission to use this command!"))
            .message(LiteBukkitMessages.PLAYER_ONLY, Format.error("Only players can execute this command!"))
            .message(LiteBukkitMessages.CONSOLE_ONLY, Format.error("Only the console can execute this command!"))
            .message(LiteBukkitMessages.WORLD_NOT_EXIST, Format.error("Specified world does not exist!"))
            .message(LiteBukkitMessages.LOCATION_INVALID_FORMAT, Format.error("Location is not formatted correctly!"))
            .message(LiteBukkitMessages.PLAYER_NOT_FOUND, Format.error("Player was not found!"))
            .build()
    }
}