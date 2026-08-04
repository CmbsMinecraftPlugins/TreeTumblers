package xyz.devcmb.tumblers.controllers.server

import io.leangen.geantyref.TypeToken
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.PaperCommandManager
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.commands.dev.DebugCommand
import xyz.devcmb.tumblers.commands.dev.ItemCommand
import xyz.devcmb.tumblers.commands.dev.NametagCommand
import xyz.devcmb.tumblers.commands.dev.PackCommand
import xyz.devcmb.tumblers.commands.dev.QibCommand
import xyz.devcmb.tumblers.commands.dev.SpectateCommand
import xyz.devcmb.tumblers.commands.dev.TimerCommand
import xyz.devcmb.tumblers.commands.dev.WorldCommand
import xyz.devcmb.tumblers.commands.event.EventCommand
import xyz.devcmb.tumblers.commands.games.BadgeCommand
import xyz.devcmb.tumblers.commands.games.GameCommand
import xyz.devcmb.tumblers.commands.misc.ChatCommand
import xyz.devcmb.tumblers.commands.organizer.ScoreCommand
import xyz.devcmb.tumblers.commands.organizer.TeamCommand
import xyz.devcmb.tumblers.commands.organizer.WhitelistCommand
import xyz.devcmb.tumblers.commands.parsers.TumblingPlayerArgumentParser
import xyz.devcmb.tumblers.controllers.IController
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.data.TumblingPlayer

@Controller(Controller.Priority.LOWEST)
object CommandController : IController {
    lateinit var commandManager: PaperCommandManager<CommandSourceStack>
    lateinit var annotationParser: AnnotationParser<CommandSourceStack>

    override fun init() {
        commandManager = PaperCommandManager.builder()
            .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
            .buildOnEnable(TreeTumblers.plugin)

        commandManager.parserRegistry().registerParserSupplier(TypeToken.get(TumblingPlayer::class.java)) {
            TumblingPlayerArgumentParser()
        }

        annotationParser = AnnotationParser(commandManager, CommandSourceStack::class.java)
        annotationParser.parse(
            DebugCommand(),
            ItemCommand(),
            NametagCommand(),
            PackCommand(),
            QibCommand(),
            SpectateCommand(),
            TimerCommand(),
            WorldCommand(),
            EventCommand(),
            BadgeCommand(),
            GameCommand(),
            ChatCommand(),
            ScoreCommand(),
            TeamCommand(),
            WhitelistCommand()
        )

        GameController.games.forEach { game ->
            game.data.builderCommand?.let { annotationParser.parse(it) }
        }
    }
}