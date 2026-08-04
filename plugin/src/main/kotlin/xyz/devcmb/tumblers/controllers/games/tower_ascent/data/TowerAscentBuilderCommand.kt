package xyz.devcmb.tumblers.controllers.games.tower_ascent.data

import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.world.block.BlockTypes
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.commands.requirePlayer
import xyz.devcmb.tumblers.controllers.games.tower_ascent.feature.TowerGenerator
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.clipboard
import xyz.devcmb.tumblers.util.getPivot
import java.io.File
import java.io.FileOutputStream
import kotlin.io.path.Path

@Suppress("unused")
@Permission("tumbling.dev")
object TowerAscentBuilderCommand {
    @Command("btools tower_ascent save_room <mapId> <name>")
    fun saveRoom(
        source: CommandSourceStack,
        @Argument(suggestions = "tower_ascent_maps") mapId: String,
        name: String,
        @Flag("confirm") confirm: Boolean,
    ) {
        val player = source.sender.requirePlayer() ?: return
        val map = TowerAscentData.maps.find { it.id == mapId }
        if(map == null) {
            player.sendMessage(Format.error("Map with the provided ID was not found!"))
            return
        }

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

    @Suggestions("tower_ascent_maps")
    fun suggestMaps(): List<String> {
        return TowerAscentData.maps.map { it.id }
    }

    // TODO: Add load command
}