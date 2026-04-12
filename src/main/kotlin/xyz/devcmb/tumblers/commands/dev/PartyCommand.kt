package xyz.devcmb.tumblers.commands.dev

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.block.BlockTypes
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.party.PartyController
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import java.io.File
import java.io.FileOutputStream
import kotlin.io.path.Path

@Command(name = "party")
class PartyCommand {
    @Execute(name = "template save")
    fun executeTemplateSave(@Context player: Player, @Arg("party game") partyGame: PartyController.PartyGame, @Arg("identifier") identifier: String) {
        val worldEdit = WorldEdit.getInstance()
        val sessionManager = worldEdit.sessionManager

        val playerSession = sessionManager.get(BukkitAdapter.adapt(player))
        var clipboard: Clipboard
        try {
            clipboard = playerSession.clipboard.clipboards.lastOrNull()
                ?: throw IllegalStateException("No clipboard loaded for this session")
        } catch(e: Exception) {
            player.sendMessage(Format.warning("Your worldedit clipboard is empty!"))
            return
        }

        if(clipboard.region.volume == 0L) {
            player.sendMessage(Format.warning("Your worldedit clipboard is empty!"))
            return
        }

        var pivot: BlockVector3? = null
        for(loc in clipboard.region) {
            if(clipboard.getBlock(loc).blockType == BlockTypes.DIAMOND_BLOCK) {
                pivot = loc
                break
            }
        }

        if(pivot == null) {
            player.sendMessage(Format.error("Your clipboard does not have a diamond block pivot point!"))
            return
        }

        clipboard.origin = pivot

        DebugUtil.info("Started party template save job")
        player.sendMessage(Format.info("Started party template save job..."))

        val saveFile = File(Path(PartyController.partyGamesDirectory, partyGame.id, "$identifier.schem").toString())
        val parent = saveFile.parentFile
        if(!parent.exists() && !parent.mkdirs()) {
            DebugUtil.severe("Failed to create directory for party template save job. game id: ${partyGame.id}, mapName: $identifier")
            player.sendMessage(Format.error("Directory creation failed!"))
            return
        }

        TreeTumblers.pluginScope.launch {
            withContext(Dispatchers.IO) {
                FileOutputStream(saveFile).use {
                    BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC
                        .getWriter(it)
                        .use { writer -> writer.write(clipboard) }
                }

                DebugUtil.success("Party template saved to ${saveFile.absolutePath} successfully")
                player.sendMessage(Format.success("Party template saved successfully!"))
            }
        }
    }

    @Execute(name = "template load")
    fun executeTemplateLoad(@Context executor: Player, @Arg("schematic") schematic: PartyController.PartyGameSchematic) {
        val format = ClipboardFormats.findByFile(schematic.file)

        if(format == null) {
            executor.sendMessage(Format.error("That is not a valid schematic file!"))
            return
        }

        DebugUtil.info("Started the load process for ${schematic.file.parentFile.name}/${schematic.file.name}")
        executor.sendMessage(Format.info("Started template load job..."))

        val clipboard: Clipboard
        format.getReader(schematic.file.inputStream()).use { reader ->
            clipboard = reader.read()
        }

        val editSession = WorldEdit.getInstance()
            .newEditSessionBuilder()
            .world(BukkitAdapter.adapt(executor.world))
            .fastMode(true)
            .build()

        val operation = ClipboardHolder(clipboard)
            .createPaste(editSession)
            .to(BukkitAdapter.adapt(executor.location).toBlockPoint())
            .ignoreAirBlocks(false)
            .build()

        Operations.complete(operation)
        editSession.flushQueue()
        editSession.close()

        DebugUtil.success("Party game schematic ${schematic.file.parentFile.name}/${schematic.file.name} loaded successfully")
        executor.sendMessage(Format.success("Party schematic loaded successfully!"))
    }
}
