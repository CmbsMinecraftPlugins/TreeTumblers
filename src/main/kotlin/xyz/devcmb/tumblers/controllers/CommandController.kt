package xyz.devcmb.tumblers.controllers

import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.annotations.Controller
import dev.rollczi.litecommands.LiteCommands
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory
import xyz.devcmb.playground.commands.arguments.*
import xyz.devcmb.playground.commands.dev.*
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.util.DebugUtil

@Controller("commandController", Controller.Priority.LOWEST)
class CommandController : IController {
    lateinit var liteCommands: LiteCommands<CommandSender>
    override fun init() {
        liteCommands = LiteBukkitFactory.builder("tumblers", TreeTumblers.plugin)
            .commands(
                DebugCommand()
            )
            .argument(DebugUtil.DebugLogLevel::class.java, DebugLogLevelArgument())
            .build()
    }
}