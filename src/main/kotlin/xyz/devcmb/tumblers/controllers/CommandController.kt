package xyz.devcmb.tumblers.controllers

import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.annotations.Controller
import dev.rollczi.litecommands.LiteCommands
import dev.rollczi.litecommands.adventure.LiteAdventureExtension
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory
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

@Controller("commandController", Controller.Priority.LOWEST)
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
                ChatCommand()
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
            .extension(LiteAdventureExtension<CommandSender>()) { config ->
                config.serializer(Format.miniMessage)
            }
            .invalidUsage(InvalidUsageHandler())
            .build()
    }
}