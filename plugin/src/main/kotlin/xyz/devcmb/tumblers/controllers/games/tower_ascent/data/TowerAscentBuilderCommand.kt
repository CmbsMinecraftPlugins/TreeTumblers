package xyz.devcmb.tumblers.controllers.games.tower_ascent.data

import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.world.block.BlockTypes
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.tower_ascent.feature.TowerGenerator
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.clipboard
import xyz.devcmb.tumblers.util.getPivot
import java.io.File
import java.io.FileOutputStream
import kotlin.io.path.Path

@Command(name = "btools tower_ascent")
@Permission("tumbling.dev")
object TowerAscentBuilderCommand {
    @Execute(name = "save_room")
    fun saveRoom(
        @Context player: Player,
        @Arg map: Map,
        @Arg("name") name: String,
        @dev.rollczi.litecommands.annotations.flag.Flag("--confirm") confirm: Boolean,
    ) {
        val clipboard = player.clipboard
        if(clipboard == null) {
            player.sendMessage(Format.error("Worldedit clipboard is empty!"))
            return
        }

        if(clipboard.region.volume == 0L) {
            player.sendMessage(Format.error("Worldedit clipboard is empty!"))
            return
        }

        if(clipboard.getPivot(BlockTypes.DIAMOND_BLOCK!!) == null) {
            player.sendMessage(Format.error("Clipboard is missing a straight line of 5 diamond blocks as the starting pivot!"))
            return
        }

        if(clipboard.getPivot(BlockTypes.REDSTONE_BLOCK!!) == null) {
            player.sendMessage(Format.error("Clipboard is missing a straight line of 5 redstone blocks as the ending pivot!"))
            return
        }

        val file = File(Path(
            TowerGenerator.templatesDirectory,
            map.id,
            "rooms",
            "$name.schem"
        ).toString())

        if(file.exists() && !confirm) {
            player.sendMessage(Format.warning("This room already exists! Re-run with --confirm to continue anyways."))
            return
        }

        player.sendMessage(Format.info("Starting tower ascent template save job..."))

        val parent = file.parentFile
        if(!parent.exists() && !parent.mkdirs()) {
            player.sendMessage(Format.error("Directory setup failed!"))
            return
        }

        TreeTumblers.pluginScope.launch {
            withContext(Dispatchers.IO) {
                FileOutputStream(file).use {
                    BuiltInClipboardFormat.FAST_V3
                        .getWriter(it)
                        .use { writer -> writer.write(clipboard) }
                }

                DebugUtil.success("Tower ascent room saved to ${file.absolutePath} successfully")
                player.sendMessage(Format.success("Tower ascent template saved successfully!"))
            }
        }
    }

    // TODO: Add load command
}