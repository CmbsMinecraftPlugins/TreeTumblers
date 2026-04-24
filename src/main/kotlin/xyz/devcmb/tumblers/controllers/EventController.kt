package xyz.devcmb.tumblers.controllers

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.session.ClipboardHolder
import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.TumblingEventException
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.engine.Timer
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.formattedName
import xyz.devcmb.tumblers.util.getPlayers
import xyz.devcmb.tumblers.util.openHandledInventory
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.validateList
import xyz.devcmb.tumblers.util.validateLocation
import java.io.File
import kotlin.math.min

@Controller("eventController", Controller.Priority.MEDIUM)
class EventController : IController {
    lateinit var teamScores: HashMap<Team, Int>

    private val databaseController: DatabaseController by lazy {
        ControllerDelegate.getController<DatabaseController>()
    }

    private val gameController: GameController by lazy {
        ControllerDelegate.getController<GameController>()
    }

    private val playerController: PlayerController by lazy {
        ControllerDelegate.getController<PlayerController>()
    }

    lateinit var topbarRunnable: BukkitRunnable
    var state: State = State.EVENT_INACTIVE

    var eventTimer: Timer? = null
    var eventTimerTitle: String? = null

    val readyCheckWaiting: ArrayList<Player> = ArrayList()
    var readyCheckAborted: Boolean = false
    var readyCheckTimer: Timer? = null

    var game: Int = 0
    val totalGames: Int
        get() {
            return min(gameController.games.filter { it.votable }.size, 8)
        }
    val playedGames: ArrayList<String> = ArrayList()
    val votingQuadrants: ArrayList<ArrayList<Location>> = ArrayList()

    companion object {
        @field:Configurable("event.event_mode")
        var eventMode: Boolean = false

        @field:Configurable("lobby.world")
        var lobbyWorld: String = "hub"

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
                return field.replace("&", TreeTumblers.plugin.dataFolder.toString())
            }
    }

    override fun init() {
        TreeTumblers.pluginScope.launch {
            teamScores = databaseController.getTeamScores()
        }

        topbarRunnable = object : BukkitRunnable() {
            override fun run() = sendDefaultTopbar()
        }
        topbarRunnable.runTaskTimer(TreeTumblers.plugin, 0, 20)
    }

    @EventHandler
    fun serverLoadEvent(event: ServerLoadEvent) {
        val lobby = Bukkit.getWorld(lobbyWorld)!!
        val votingQuadrantPositions = TreeTumblers.plugin.config.getList("event.voting.quadrants")?.map {
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

    fun startEvent() {
        TreeTumblers.pluginScope.launch {
            val ready = readyCheck()
            if(!ready) return@launch

            state = State.PRE_EVENT
            eventTimer = Timer(5) {
                id = "pre_event_timer"
                joined = true
            }
            eventTimerTitle = "Event Start"
            eventTimer!!.start()
            runOpeningCutscene()

            repeat(totalGames) {
                eventLoop()
            }

            // TODO: Finale
            cleanupEvent()
        }
    }

    fun cleanupEvent() {
        state = State.EVENT_INACTIVE
        playedGames.clear()
        game = 0
        eventTimer = null
        eventTimerTitle = null
    }

    var nextGame: String? = null
    suspend fun eventLoop() {
        game++
        voting()
        state = State.NORMAL_GAME
        gameController.startGame(nextGame!!)
        playedGames.add(nextGame!!)
        nextGame = null
        state = State.INTERMISSION
        delay(10000)
    }

    suspend fun readyCheck(): Boolean {
        Bukkit.broadcast(Format.mm("<green><b>Are you ready?</b><green>"))
        Team.entries
            .filter { it.playingTeam }
            .forEach {
                readyCheckWaiting.addAll(it.getOnlinePlayers())
            }

        suspendSync {
            readyCheckWaiting.forEach { it.openHandledInventory("readyCheckInventory") }
        }

        readyCheckTimer = Timer(10) {
            id = "ready_check_expiry"
            onComplete { early ->
                if(early) return@onComplete

                readyCheckWaiting.forEach {
                    Bukkit.broadcast(Format.error(
                        Format.mm(
                            "<player> is not ready!",
                            Placeholder.component("player", it.formattedName)
                        )
                    ))
                }
                readyCheckWaiting.clear()
                readyCheckAborted = true
            }
        }
        readyCheckTimer!!.start()

        while(readyCheckWaiting.isNotEmpty() && !readyCheckAborted) {
            delay(50)
        }

        val aborted = readyCheckAborted
        readyCheckAborted = false

        readyCheckTimer?.end()
        readyCheckTimer = null

        if(aborted) {
            suspendSync {
                Bukkit.getOnlinePlayers().forEach { it.closeInventory() }
            }
            Bukkit.broadcast(Format.warning("Not all players ready! Ready check failed!"))
        } else {
            Bukkit.broadcast(Format.success("All players ready! Ready check success!"))
        }

        return !aborted
    }

    fun markReady(player: Player) {
        Bukkit.broadcast(
            Format.mm(
                "<green><player> is ready!</green>",
                Placeholder.component("player", player.formattedName)
            )
        )
        readyCheckWaiting.remove(player)
    }

    fun markNotReady(player: Player) {
        Bukkit.broadcast(
            Format.mm(
                "<red><player> is not ready!</red>",
                Placeholder.component("player", player.formattedName)
            )
        )
        readyCheckAborted = true
        readyCheckWaiting.clear()
    }

    val cutsceneSteps: ArrayList<CutsceneStep> = arrayListOf(
        CutsceneStep(null, "first") {
            title(
                Format.mm("<green><b>Tree Tumblers</b></green>"),
                Format.mm("Welcome to the event!"),
                Title.Times.times(Tick.of(5), Tick.of(80), Tick.of(40))
            )
            delay(7000)
        }
    )

    suspend fun runOpeningCutscene() {
        // TODO: Add more steps and adjust the `time` to match
        eventTimer = Timer(7) {
            id = "event_opening_cutscene"
        }
        eventTimer!!.start()
        eventTimerTitle = "Cutscene"

        cutsceneSteps.forEach {
            val observers = Bukkit.getOnlinePlayers().toSet()
            it.run(
                observers,
                Bukkit.getWorld(lobbyWorld)!!,
                TreeTumblers.plugin.config.getConfigurationSection("event.cutscene")!!
            )

            if(eventTimer?.paused == true) {
                while(eventTimer?.paused == true) {
                    delay(500)
                }
            }

            it.cleanup(observers)
        }
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

    val quadrantGames: HashMap<Int, GameController.RegisteredGame> = HashMap()
    val quadrantLogoDisplays: HashMap<Int, TextDisplay> = HashMap()
    val votes: ArrayList<Int> = ArrayList()

    suspend fun voting() {
        val lobby = Bukkit.getWorld(lobbyWorld)!!
        val logoLocations: ArrayList<Location> = ArrayList()

        val logoPositions = TreeTumblers.plugin.config.getList("event.voting.logos")
            ?.map {
                if(it !is List<*>) throw TumblingEventException("Voting logo positions is not a 2d list")
                it.take(3).validateList<Int>() ?: throw TumblingEventException("Voting logo positions do not contain exclusively Integers")
            } ?: throw TumblingEventException("Voting logo positions not provided")

        val logoQuaternions = TreeTumblers.plugin.config.getList("event.voting.logos")
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

        state = State.VOTING

        suspendSync {
            Bukkit.getOnlinePlayers().forEach {
                val location = voteCenter.validateLocation(Bukkit.getWorld(lobbyWorld)!!)
                    ?: throw TumblingEventException("Voting arena does not have a center location")

                it.teleport(location.toCenterLocation())
            }
        }

        val originalBlocks: HashMap<Location, Material> = HashMap()
        eventTimer = Timer(20) {
            id = "event_voting"
            joined = true

            timeExecution(2) {
                val domeFrom = domeStart.validateLocation(Bukkit.getWorld(lobbyWorld)!!)
                    ?: throw TumblingEventException("Dome from coordinates aren't valid!")
                val domeTo = domeEnd.validateLocation(Bukkit.getWorld(lobbyWorld)!!)
                    ?: throw TumblingEventException("Dome to coordinates aren't valid!")

                val blocks: ArrayList<Location> = ArrayList()
                domeFrom.forEachRegion(domeTo) {
                    if(it.type != quadrantSeparator) return@forEachRegion
                    blocks.add(it.location.clone())
                }

                repeat(3) { i ->
                    suspendSync {
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
        eventTimerTitle = "Voting"

        repeat(4) {
            val quadrant = votingQuadrants[it]
            val games = gameController.games.filter { game -> !playedGames.contains(game.id) && !quadrantGames.containsValue(game) }
            if(games.isEmpty()) return@repeat

            val game = games.random()
            quadrantGames.put(it, game)
            suspendSync {
                loadDiorama(game.id, it)
            }

            suspendSync {
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
                    display.brightness = Display.Brightness(15, 15)
                }

                quadrantLogoDisplays[it] = logoDisplay
            }

            Audience.audience(Bukkit.getOnlinePlayers()).showTitle(Title.title(
                Component.text(game.name, votingTextColors[it]),
                Component.empty(),
                Title.Times.times(Tick.of(0), Tick.of(70), Tick.of(5))
            ))

            delay(2500)
        }

        delay(2000)

        Audience.audience(Bukkit.getOnlinePlayers()).showTitle(Title.title(
            Format.mm("<green>Vote!</green>"),
            Component.empty(),
            Title.Times.times(Tick.of(5), Tick.of(40), Tick.of(5))
        ))

        eventTimer!!.start()

        Audience.audience(Bukkit.getOnlinePlayers()).showTitle(Title.title(
            Format.mm("<yellow>Voting Closed!</yellow>"),
            Component.empty(),
            Title.Times.times(Tick.of(5), Tick.of(40), Tick.of(5))
        ))

        suspendSync {
            votingQuadrants.forEach { quadrant ->
                quadrant.forEach { loc ->
                    loc.block.type = inactiveQuadrantMaterial
                }
            }
        }

        val winningGame = countVotes()

        delay(2000)

        Audience.audience(Bukkit.getOnlinePlayers()).showTitle(Title.title(
            Component.empty(),
            Component.text("And the game is..."),
            Title.Times.times(Tick.of(0), Tick.of(99999), Tick.of(5))
        ))

        delay(2000)

        Audience.audience(Bukkit.getOnlinePlayers()).showTitle(Title.title(
            winningGame.first.logo,
            Component.text("And the game is..."),
            Title.Times.times(Tick.of(0), Tick.of(60), Tick.of(20))
        ))

        suspendSync {
            originalBlocks.forEach {
                it.key.block.type = it.value
            }
            cleanupDioramas()
        }

        var votesComponent = Format.mm("<white><bold>Votes </bold><br></white>")

        suspendSync {
            quadrantGames.forEach { it
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
        nextGame = winningGame.first.id
        delay(7000)

        suspendSync {
            quadrantLogoDisplays.forEach {
                it.value.remove()
            }

            quadrantLogoDisplays.clear()
        }
    }

    val currentDioramaSessions: ArrayList<EditSession> = ArrayList()
    fun loadDiorama(id: String, index: Int) {
        val schematic = File(dioramasFolder, "$id.schem")
        if(!schematic.exists()) {
            DebugUtil.warning("Could not find a diorama schematic for $id, aborting")
            return
        }

        val format = ClipboardFormats.findByFile(schematic)
        if(format == null) {
            DebugUtil.warning("${schematic.parentFile.name}/${schematic.name} is not a valid schematic, aborting")
            return
        }

        val clipboard: Clipboard
        format.getReader(schematic.inputStream()).use { reader ->
            clipboard = reader.read()
        }

        val lobbyWorld = Bukkit.getWorld(lobbyWorld)!!
        clipboard.origin = BukkitAdapter.adapt(voteCenter.validateLocation(lobbyWorld)).toBlockPoint()

        val editSession = WorldEdit.getInstance()
            .newEditSessionBuilder()
            .world(BukkitAdapter.adapt(lobbyWorld))
            .build()

        val holder = ClipboardHolder(clipboard)
        holder.transform = holder.transform.combine(
            AffineTransform().rotateY(index * -90.0)
        )

        try {
            val operation = holder
                .createPaste(editSession)
                .to(BukkitAdapter.adapt(voteCenter.validateLocation(lobbyWorld)).toBlockPoint())
                .ignoreAirBlocks(true)
                .build()

            Operations.complete(operation)
            editSession.flushQueue()

            currentDioramaSessions.add(editSession)
        } catch(e: Exception) {
            editSession.close()
            DebugUtil.severe("Failed to load game diorama for $id: ${e.message}")
        }
    }

    fun cleanupDioramas() {
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

    fun countVotes(): Pair<GameController.RegisteredGame, Int> {
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

    fun sendDefaultTopbar() {
        var teamComponent = Component.empty()

        getEventTeamPlacements().forEachIndexed { i, it ->
            var playersComponent = Component.text(" ")
            databaseController.whitelistedPlayersCache.toSortedMap().filter { entry -> entry.value == it.first }.toList().forEachIndexed { index, entry ->
                val uuid = databaseController.whitelistedPlayerUUIDs[entry.first]!!
                val onlinePlayer = Bukkit.getPlayer(uuid)
                var name = Component.empty()
                if(index != 0) {
                    name = name.append(Component.text(" • ", NamedTextColor.WHITE))
                }

                name = name.append(Format.mm("<color:${if(onlinePlayer != null) "white" else "dark_gray"}><head:$uuid></color> ")).append(
                    if(onlinePlayer != null) Component.text(onlinePlayer.name, it.first.color)
                    else Component.text(entry.first, NamedTextColor.GRAY)
                )

                playersComponent = playersComponent.append(name)
            }
            playersComponent = playersComponent.append(Component.text(" "))

            teamComponent = teamComponent.append(
                Format.mm(
                    " <br><white>#${it.second}</white> <team><shift>${" ".repeat(60)}<gold>${teamScores[it.first]!!}</gold> <br><players><br>",
                    Placeholder.component("team", it.first.formattedName),
                    // By negative spacing, I don't need to do any math for the repetition (and don't need to use periods)
                    Placeholder.component("shift", UserInterfaceUtility.negativeSpace(UserInterfaceUtility.getPixelWidth(it.first.teamName) + 11)),
                    Placeholder.component("players", playersComponent)
                )
            )
        }

        Bukkit.getOnlinePlayers().forEach {
            it.sendPlayerListHeader(
                Component.empty()
                    .appendNewline()
                    .append(Component.text("\u0001").font(UserInterfaceUtility.ICONS))
                    .appendNewline()
                    .appendNewline()
                    .appendNewline()
                    .appendNewline()
                    .appendNewline()
                    .append(
                        Component.text("EVENT MODE: ", NamedTextColor.AQUA)
                            .append(Component.text(
                                if(eventMode) "ENABLED" else "DISABLED",
                                if(eventMode) NamedTextColor.GREEN else NamedTextColor.RED
                            ))
                    )
                    .appendNewline()
                    .append(
                        Component.text("DATABASE MODE: ", NamedTextColor.YELLOW)
                            .append(Component.text(
                                if(DatabaseController.enabled) "ENABLED" else "DISABLED",
                                if(DatabaseController.enabled) NamedTextColor.GREEN else NamedTextColor.RED
                            ))
                    )
                    .appendNewline()
                    .append(teamComponent)
            )

            it.sendPlayerListFooter(Format.mm(
                "<aqua>Ping: <white>${it.ping}ms</white></aqua><br><aqua>Online Players: <white>${Bukkit.getOnlinePlayers().size}</white></aqua><br>"
            ))
        }
    }

    fun grantScore(player: TumblingPlayer, amount: Int) {
        player.score += amount
        teamScores.put(player.team, (teamScores[player.team] ?: 0) + amount)
    }

    fun getEventTeamPlacements(): ArrayList<Pair<Team, Int>> {
        val sorted = teamScores.entries.sortedWith(
            compareByDescending<MutableMap.MutableEntry<Team, Int>> { it.value }
                .thenBy { it.key.priority }
        )
        return MiscUtils.calculatePlacements(sorted)
    }

    fun getEventPlayerPlacements(): ArrayList<Pair<TumblingPlayer, Int>> {
        val playerScores: HashMap<TumblingPlayer, Int> = HashMap()
        playerController.players.forEach {
            playerScores.put(it, it.score)
        }

        val sorted = playerScores.entries.sortedWith(
            compareByDescending<MutableMap.MutableEntry<TumblingPlayer, Int>> { it.value }
                .thenBy { it.key.bukkitPlayer?.name }
        )
        return MiscUtils.calculatePlacements(sorted)
    }

    fun replicateScores() {
        if(!eventMode) return

        TreeTumblers.pluginScope.launch {
            DebugUtil.info("Replicating event data...")
            databaseController.replicateTeamData(teamScores)
            playerController.players.forEach {
                databaseController.replicatePlayerData(it)
            }
            DebugUtil.success("Data replication successful!")
        }
    }

    override fun cleanup() {
        replicateScores()
    }

    enum class State {
        EVENT_INACTIVE,
        PRE_EVENT,
        INTERMISSION,
        VOTING,
        NORMAL_GAME,
        FINAL_GAME,
        POST_EVENT
    }
}