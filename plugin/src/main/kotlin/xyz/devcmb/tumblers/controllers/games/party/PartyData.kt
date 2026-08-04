package xyz.devcmb.tumblers.controllers.games.party

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.block.BlockTypes
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.incendo.cloud.annotation.specifier.Quoted
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.commands.requirePlayer
import xyz.devcmb.tumblers.controllers.games.party.PartyController.PartyScoreSource
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.clipboard
import java.io.File
import java.io.FileOutputStream
import kotlin.io.path.Path

object PartyData : GameData(
    id = "party",
    name = "Party",
    votable = true,
    maps = setOf(
        Map("main")
    ),
    cutsceneSteps = arrayListOf(
        CutsceneStep(Component.empty()
            .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
            .append(Format.mm("<glyph:game/party_icon>"))
            .append(Component.text(" Party")),
            "cutscene.start"
        ) {
            delay(5000)
        },
        CutsceneStep(
            Format.mm("In this game, <yellow>you</yellow> and <yellow>your team</yellow> will fight in head-to-head <aqua>minigames!</aqua>"),
            "cutscene.first"
        ) {
            delay(5000)
        },
        CutsceneStep(
            Format.mm("This game comes in <aqua>2 parts...</aqua>"),
            "cutscene.second"
        ) {
            delay(2500)
        },
        CutsceneStep(
            Format.mm("You start playing <yellow>individual games</yellow> where you fight one other person.<br>This stage lasts the first <aqua>5m</aqua> of the game."),
            "cutscene.third"
        ) {
            delay(5000)
        },
        CutsceneStep(
            Format.mm("Then, you will transition to playing <yellow>team games</yellow> where you fight against a whole team.<br>This stage lasts the final <aqua>5m</aqua> of the game."),
            "cutscene.fourth"
        ) {
            delay(5000)
        },
        CutsceneStep(
            Format.mm("Game range from <aqua>Sword duels</aqua> to <aqua>Mace duels</aqua> and anything in between!<br>While you're waiting for a match, you'll be waiting here."),
            "cutscene.start"
        ) {
            delay(5000)
        },
        CutsceneStep.GLHF
    ),
    scores = hashMapOf(
        PartyScoreSource.INDIVIDUAL_GAME_WIN to 80,
        PartyScoreSource.INDIVIDUAL_GAME_DRAW to 40,
        PartyScoreSource.TEAM_GAME_WIN to 240,
        PartyScoreSource.TEAM_GAME_DRAW to 160
    ),
    flags = setOf(
        Flag.DISABLE_FALL_DAMAGE,
        Flag.DISABLE_BLOCK_BREAKING,
        Flag.DISABLE_NATURAL_REGENERATION
    ),
    scoreboard = PartyScoreboard::class,
) {
    override val builderCommand =
        @Suppress("unused")
        @Permission("tumbling.dev")
        object {
            @Command("btools party save_room <partyGameId> <mapId>")
            fun saveRoom(
                source: CommandSourceStack,
                @Argument(suggestions = "party_game_ids") partyGameId: String,
                mapId: String
            ) {
                val player = source.sender.requirePlayer() ?: return
                if(partyGameId !in PartyController.gameIds) {
                    player.sendMessage(Format.error("A party game with the provided ID was not found"))
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

                val saveFile = File(Path(PartyController.partyGamesDirectory, partyGameId, "$mapId.schem").toString())
                val parent = saveFile.parentFile
                if(!parent.exists() && !parent.mkdirs()) {
                    DebugUtil.severe("Failed to create directory for party template save job. game id: $partyGameId, mapName: $mapId")
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

            @Command("btools party load_room <schematic>")
            fun loadRoom(
                source: CommandSourceStack,
                @Quoted @Argument(suggestions = "party_game_schematics") schematic: String
            ) {
                val player = source.sender.requirePlayer() ?: return

                val schematicFile = File(PartyController.partyGamesDirectory, schematic)
                if(!schematicFile.exists()) {
                    player.sendMessage(Format.error("Provided schematic path does not exist!"))
                    return
                }

                val format = ClipboardFormats.findByFile(schematicFile)

                if(format == null) {
                    player.sendMessage(Format.error("Provided file was not detected to be a schematic!"))
                    return
                }

                DebugUtil.info("Started the load process for ${schematicFile.parentFile.name}/${schematicFile.name}")
                player.sendMessage(Format.info("Started template load job..."))

                val clipboard: Clipboard
                format.getReader(schematicFile.inputStream()).use { reader ->
                    clipboard = reader.read()
                }

                val editSession = WorldEdit.getInstance()
                    .newEditSessionBuilder()
                    .world(BukkitAdapter.adapt(player.world))
                    .fastMode(true)
                    .build()

                val operation = ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BukkitAdapter.adapt(player.location).toBlockPoint())
                    .ignoreAirBlocks(false)
                    .build()

                Operations.complete(operation)
                editSession.flushQueue()
                editSession.close()

                DebugUtil.success("Party game schematic ${schematicFile.parentFile.name}/${schematicFile.name} loaded successfully")
                player.sendMessage(Format.success("Party schematic loaded successfully!"))
            }

            private fun getPaths(): ArrayList<String> {
                val suggestions: ArrayList<String> = ArrayList()
                val searchDir = File(PartyController.partyGamesDirectory)

                searchDir.listFiles()!!.forEach { parent ->
                    if(parent.isDirectory) {
                        parent.listFiles()!!.forEach {
                            if(!it.isDirectory) {
                                suggestions.add("${parent.name}/${it.name}")
                            }
                        }
                    }
                }

                return suggestions
            }

            @Suggestions("party_game_schematics")
            fun suggestPartyGameSchematics(): List<String> {
                return getPaths().map { "\"$it\"" }
            }

            @Suggestions("party_game_ids")
            fun suggestPartyGameIDs(): List<String> {
                return PartyController.gameIds
            }
        }
}