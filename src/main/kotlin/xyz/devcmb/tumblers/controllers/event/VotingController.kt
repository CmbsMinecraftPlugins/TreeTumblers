package xyz.devcmb.tumblers.controllers.event

import com.fastasyncworldedit.core.extent.processor.lighting.RelightMode
import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.session.ClipboardHolder
import io.papermc.paper.util.Tick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.TumblingEventException
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.ControllerBase
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.player.MusicController
import xyz.devcmb.tumblers.engine.Timer
import xyz.devcmb.tumblers.util.Benchmark
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.getPlayers
import xyz.devcmb.tumblers.util.tp
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.validateList
import xyz.devcmb.tumblers.util.validateLocation
import java.io.File
import kotlin.collections.forEach
import kotlin.collections.set
import kotlin.collections.take
import kotlin.collections.takeLast

@Controller(Controller.Priority.LOWEST)
class VotingController : ControllerBase() {
    companion object {
        @field:Configurable("event.voting.inactive_quadrant_material")
        var inactiveQuadrantMaterial: Material = Material.GRAY_CONCRETE

        @field:Configurable("event.voting.center")
        var voteCenter: List<Int> = listOf(0,0,0)

        @field:Configurable("event.voting.quadrant_separator")
        var quadrantSeparator: Material = Material.SMOOTH_QUARTZ_SLAB

        @field:Configurable("event.voting.quadrant_replacement")
        var quadrantReplacement: Material = Material.SMOOTH_QUARTZ

        @field:Configurable("event.voting.dome_start")
        var domeStart: List<Int> = listOf(-8,189,24)

        @field:Configurable("event.voting.dome_end")
        var domeEnd: List<Int> = listOf(26,197,-11)

        @field:Configurable("templates.dioramas")
        var dioramasFolder: String = "&/templates/dioramas"
            get() {
                return field.replace("&", TreeTumblers.Companion.plugin.dataFolder.toString())
            }

        @field:Configurable("lobby.world")
        var lobbyWorld: String = "hub"
    }

    val eventController: EventController by controller()
    val musicController: MusicController by controller()
    val gameController: GameController by controller()

    val votingQuadrants: ArrayList<ArrayList<Location>> = ArrayList()
    val quadrantGames: HashMap<Int, GameController.RegisteredGame> = HashMap()
    val quadrantLogoDisplays: HashMap<Int, TextDisplay> = HashMap()
    val votes: ArrayList<Int> = ArrayList()
    val currentDioramaSessions: ArrayList<EditSession> = ArrayList()

    val votingOn: Boolean
        get() {
            return eventController.state == EventController.State.VOTING
        }

    val votingTextColors: ArrayList<TextColor> = arrayListOf(
        NamedTextColor.RED,
        NamedTextColor.BLUE,
        NamedTextColor.GREEN,
        NamedTextColor.YELLOW
    )

    val votingConcretes: ArrayList<Material> = arrayListOf(
        Material.RED_CONCRETE,
        Material.BLUE_CONCRETE,
        Material.LIME_CONCRETE,
        Material.YELLOW_CONCRETE
    )

    override fun init() {
    }

    suspend fun startVoting(): String {
        val lobby = Bukkit.getWorld(lobbyWorld)!!
        val logoLocations: ArrayList<Location> = ArrayList()

        val logoPositions = TreeTumblers.Companion.plugin.config.getList("event.voting.logos")
            ?.map {
                if(it !is List<*>) throw TumblingEventException("Voting logo positions is not a 2d list")
                it.take(3).validateList<Int>() ?: throw TumblingEventException("Voting logo positions do not contain exclusively Integers")
            } ?: throw TumblingEventException("Voting logo positions not provided")

        val logoQuaternions = TreeTumblers.Companion.plugin.config.getList("event.voting.logos")
            ?.map {
                if(it !is List<*>) throw TumblingEventException("Voting logo positions is not a 2d list")
                val list: List<Float> = it.takeLast(4)
                    .validateList<Double>()
                    ?.map { entry -> entry.toFloat() }
                    ?: throw TumblingEventException("Voting logo positions do not contain exclusively Floats")

                Quaternionf(list[0], list[1], list[2], list[3])
            } ?: throw TumblingEventException("Voting logo quaternions not provided")

        logoPositions.forEach {
            val location = it.validateLocation(lobby) ?: throw TumblingEventException("Voting logo position is an invalid location")
            logoLocations.add(location)
        }

        musicController.playMusic(MusicController.Music.VOTING)

        MiscUtils.suspendSync {
            Bukkit.getOnlinePlayers().forEach {
                val location = voteCenter.validateLocation(Bukkit.getWorld(lobbyWorld)!!)
                    ?: throw TumblingEventException("Voting arena does not have a center location")

                it.inventory.clear()
                it.tp(location.toCenterLocation())
            }
        }

        val originalBlocks: HashMap<Location, Material> = HashMap()
        eventController.eventTimer = Timer(20) {
            id = "event_voting"
            joined = true

            timeExecution(2) {
                val domeFrom = domeStart.validateLocation(Bukkit.getWorld(lobbyWorld)!!)
                    ?: throw TumblingEventException("Dome from coordinates aren't valid!")
                val domeTo = domeEnd.validateLocation(Bukkit.getWorld(lobbyWorld)!!)
                    ?: throw TumblingEventException("Dome to coordinates aren't valid!")

                val blocks: ArrayList<Location> = ArrayList()
                domeFrom.forEachRegion(domeTo) {
                    if (it.type != quadrantSeparator) return@forEachRegion
                    blocks.add(it.location.clone())
                }

                repeat(3) { i ->
                    MiscUtils.suspendSync {
                        blocks.forEach { base ->
                            val location = Location(base.world, base.x, base.y + i, base.z)
                            originalBlocks.put(location, location.block.type)
                            location.block.type = quadrantReplacement
                        }
                    }

                    delay(500)
                }
            }
        }
        eventController.eventTimerTitle = "Voting"

        repeat(4) {
            val quadrant = votingQuadrants[it]
            val games = gameController.games.filter { game -> !eventController.playedGames.contains(game.id) && !quadrantGames.containsValue(game) && game.votable }
            if(games.isEmpty()) return@repeat

            val game = games.random()
            quadrantGames.put(it, game)
            TreeTumblers.Companion.pluginScope.launch {
                loadDiorama(game.id, it)
            }

            MiscUtils.suspendSync {
                quadrant.forEach { loc ->
                    loc.block.type = votingConcretes[it]
                }

                val logoDisplay = lobby.spawn(logoLocations[it], TextDisplay::class.java) { display ->
                    display.text(game.logo)
                    display.transformation = Transformation(
                        Vector3f(),
                        logoQuaternions[it],
                        Vector3f(6.0f, 6.0f, 6.0f),
                        Quaternionf(0f, 0f, 0f, 1f)
                    )
                    display.isDefaultBackground = false
                    display.backgroundColor = Color.fromARGB(0)
                    display.brightness = Display.Brightness(15, 15)
                }

                quadrantLogoDisplays[it] = logoDisplay
            }

            Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
                Title.title(
                    Component.text(game.name, votingTextColors[it]),
                    Component.empty(),
                    Title.Times.times(Tick.of(0), Tick.of(70), Tick.of(5))
                ))

            delay(2500)
        }

        delay(2000)

        Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
            Title.title(
                Format.mm("<green>Vote!</green>"),
                Component.empty(),
                Title.Times.times(Tick.of(5), Tick.of(40), Tick.of(5))
            ))

        eventController.eventTimer!!.start()

        Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
            Title.title(
                Format.mm("<yellow>Voting Closed!</yellow>"),
                Component.empty(),
                Title.Times.times(Tick.of(5), Tick.of(40), Tick.of(5))
            ))

        MiscUtils.suspendSync {
            votingQuadrants.forEach { quadrant ->
                quadrant.forEach { loc ->
                    loc.block.type = inactiveQuadrantMaterial
                }
            }
        }

        val winningGame = countVotes()

        delay(2000)

        Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
            Title.title(
                Component.empty(),
                Component.text("And the game is..."),
                Title.Times.times(Tick.of(0), Tick.of(99999), Tick.of(5))
            ))

        delay(2000)

        Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
            Title.title(
                winningGame.first.logo,
                Component.text("And the game is..."),
                Title.Times.times(Tick.of(0), Tick.of(60), Tick.of(20))
            ))

        MiscUtils.suspendSync {
            originalBlocks.forEach {
                it.key.block.type = it.value
            }
            cleanupDioramas()
        }

        var votesComponent = Format.mm("<white><bold>Votes </bold><br></white>")

        MiscUtils.suspendSync {
            quadrantGames.forEach {
                it
                if (winningGame.first.id != it.value.id) {
                    quadrantLogoDisplays[it.key]!!.remove()
                }
            }
        }

        votes.forEachIndexed { i, it ->
            votesComponent = votesComponent.append(
                Format.mm(
                    "<white><br><game> - ${it}</white>",
                    Placeholder.component("game", Component.text(quadrantGames[i]!!.name, votingTextColors[i]))
                )
            )
        }

        Bukkit.broadcast(votesComponent)

        quadrantGames.clear()
        votes.clear()
        val nextGame = winningGame.first.id
        delay(7000)

        MiscUtils.suspendSync {
            quadrantLogoDisplays.forEach {
                it.value.remove()
            }

            quadrantLogoDisplays.clear()
        }

        return nextGame
    }

    private fun countVotes(): Pair<GameController.RegisteredGame, Int> {
        var highest: Pair<Int, Int>? = null

        votingQuadrants.forEachIndexed { i, it ->
            if(quadrantGames[i] == null) return@forEachIndexed

            val players = it.getPlayers(3, 0) { it.tumblingPlayer.team.playingTeam }
            votes.add(players.size)

            if(highest == null || players.size > highest.second) {
                highest = Pair(i, players.size)
            }
        }

        val randomFallback = (0..quadrantGames.size).random()
        return Pair(highest?.let { quadrantGames[highest.first]!! } ?: quadrantGames[randomFallback]!!, highest?.first ?: randomFallback)
    }

    private suspend fun loadDiorama(id: String, index: Int) = withContext(Dispatchers.IO) {
        Benchmark("diorama_loading") {
            val schematic = File(dioramasFolder, "$id.schem")
            if (!schematic.exists()) {
                DebugUtil.warning("Could not find a diorama schematic for $id, aborting")
                return@Benchmark
            }

            val format = ClipboardFormats.findByFile(schematic)
            if (format == null) {
                DebugUtil.warning("${schematic.parentFile.name}/${schematic.name} is not a valid schematic, aborting")
                return@Benchmark
            }

            val clipboard: Clipboard
            format.getReader(schematic.inputStream()).use { reader ->
                clipboard = reader.read()
            }

            completeStep("fs")

            val lobbyWorld = Bukkit.getWorld(lobbyWorld)!!
            clipboard.origin = BukkitAdapter.adapt(voteCenter.validateLocation(lobbyWorld)).toBlockPoint()

            val editSession = WorldEdit.getInstance()
                .newEditSessionBuilder()
                .world(BukkitAdapter.adapt(lobbyWorld))
                .relightMode(RelightMode.NONE)
                .build()

            val holder = ClipboardHolder(clipboard)
            holder.transform = holder.transform.combine(
                AffineTransform().rotateY(index * -90.0)
            )

            val operation = holder
                .createPaste(editSession)
                .to(BukkitAdapter.adapt(voteCenter.validateLocation(lobbyWorld)).toBlockPoint())
                .ignoreAirBlocks(true)
                .build()

            completeStep("operation")

            MiscUtils.suspendSync {
                try {
                    Operations.complete(operation)
                    editSession.flushQueue()

                    currentDioramaSessions.add(editSession)
                    completeStep("paste")
                } catch (e: Exception) {
                    editSession.close()
                    DebugUtil.severe("Failed to load game diorama for $id: ${e.message}")
                }
            }

            yieldCompletion(listOf("fs", "operation", "paste"))
        }.run()
    }

    private fun cleanupDioramas() {
        val lobbyWorld = Bukkit.getWorld(lobbyWorld)!!
        val undoSession = WorldEdit.getInstance()
            .newEditSessionBuilder()
            .world(BukkitAdapter.adapt(lobbyWorld))
            .fastMode(true)
            .build()

        currentDioramaSessions.forEach {
            it.undo(undoSession)
        }
        currentDioramaSessions.clear()

        undoSession.flushQueue()
        undoSession.close()
    }

    override fun serverLoad() {
        val lobby = Bukkit.getWorld(lobbyWorld)!!
        val votingQuadrantPositions = TreeTumblers.Companion.plugin.config.getList("event.voting.quadrants")?.map {
            if(it !is List<*>) throw TumblingEventException("Voting quadrant is not a 2d list")
            it.validateList<Int>() ?: throw TumblingEventException("Voting quadrant does not contain exclusively Integers")
        } ?: throw TumblingEventException("Voting quadrant positions not provided")

        votingQuadrantPositions.forEach {
            val from = it.take(3).validateLocation(lobby)
                ?: throw TumblingEventException("First 3 elements of the voting quadrant location are not a valid location")

            val to = it.takeLast(3).validateLocation(lobby)
                ?: throw TumblingEventException("Last 3 elements of the voting quadrant location are not a valid location")

            val blocks: ArrayList<Location> = ArrayList()
            from.forEachRegion(to) { block ->
                if(block.type == inactiveQuadrantMaterial) {
                    blocks.add(block.location)
                }
            }

            votingQuadrants.add(blocks)
        }
    }

    @EventHandler
    fun playerDamageEvent(event: EntityDamageEvent) {
        if(votingOn) {
            event.damage = 0.0
        }
    }
}