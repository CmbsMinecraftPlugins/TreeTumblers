package xyz.devcmb.tumblers.controllers.games.crumble

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.BlockVector3
import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.GameOperatorException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.EventController
import xyz.devcmb.tumblers.controllers.games.crumble.kits.*
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.DebugToolkit
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.score.CommonScoreSource
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.score.ScoreSource
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.disableBossBar
import xyz.devcmb.tumblers.util.enableBossBar
import xyz.devcmb.tumblers.util.item.AdvancedItemStack
import xyz.devcmb.tumblers.util.openHandledInventory
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.unpackCoordinates
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@EventGame
class CrumbleController : GameBase(
    id = "crumble",
    name = "Crumble",
    votable = true,
    maps = setOf(
        Map("warfare")
    ),
    cutsceneSteps = arrayListOf(
        CutsceneStep(
            Component.empty()
                .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
                .append(Component.text("\uEA00").font(NamespacedKey("tumbling", "games/crumble")))
                .append(Component.text(" Crumble"))
        ) { map ->
            teleportConfig("cutscene.start")
            delay(5000)
        },
        CutsceneStep(Format.mm("In this game, as time goes on, the map will <yellow>crumble away</yellow> in a circle")) { map ->
            teleportConfig("cutscene.crumble_demonstration")

            val center = getLocation("centers.cutscene")
            val currentMapSize = map.data.getInt("map_size")
            var currentCrumbleRadius = ((currentMapSize * sqrt(2.0) * 0.5) + 1)

            // maybe extract this duplicate code
            val runnable = object : BukkitRunnable() {
                override fun run() {
                    val radiusSquared = currentCrumbleRadius * currentCrumbleRadius

                    val halfMap = currentMapSize / 2
                    val xStart = (center.x - halfMap).toInt()
                    val xEnd = (center.x + halfMap).toInt()
                    val zStart = (center.z - halfMap).toInt()
                    val zEnd = (center.z + halfMap).toInt()
                    val yStart = (center.y - 50).toInt()
                    val yEnd = (center.y + 10).toInt()

                    for (x in xStart..xEnd) {
                        val dx = x + 0.5 - center.x
                        for (z in zStart..zEnd) {
                            val dz = z + 0.5 - center.z
                            val distSq = dx*dx + dz*dz
                            if (distSq < radiusSquared) continue

                            for (y in yStart..yEnd) {
                                val block = center.world.getBlockAt(x, y, z)
                                if (!block.type.isAir) {
                                    block.type = Material.AIR
                                }
                            }
                        }
                    }

                    currentCrumbleRadius -= 0.2
                }
            }
            runnable.runTaskTimer(TreeTumblers.plugin, 0, 1)

            delay(4000)
            runnable.cancel()
        },
        CutsceneStep(Format.mm("This game was originally designed by <click:open_url:https://www.youtube.com/@MatMart><u><red>Mat</red><white>Mart</white></u></click>, coded by <click:open_url:https://blackilykat.dev><u><color:#e09cff>Blackilykat</color></u></click>, and funded by <click:open_url:https://www.youtube.com/@Cobgd><color:#ff701e><u>GDCob</u></color></click>!")) { map ->
            teleportConfig("cutscene.credit")
            delay(4000)
        },
        CutsceneStep(Format.mm("<b><green>Good Luck, Have Fun!</green></b>")) {}
    ),
    flags = setOf(
        Flag.SURVIVAL_MODE,
        Flag.USE_SPECTATOR_DEATH_SYSTEM
    ),
    scores = hashMapOf(
        CommonScoreSource.KILL to 45,
        CommonScoreSource.TEAM_ROUND_WIN to 480,
        CommonScoreSource.TEAM_ROUND_DRAW to 240,
        CommonScoreSource.TEAM_ROUND_LOSE to 120,
    ),
    icon = Component.text("\uEA00").font(font),
    logo = Component.text("\uEA01").font(font)
        .shadowColor(ShadowColor.none()),
    scoreboard = "crumbleScoreboard"
) {
    companion object {
        val font = NamespacedKey("tumbling", "games/crumble")
        val kitItemsKey = NamespacedKey("tumbling", "kit_item")

        @field:Configurable("games.crumble.max_kit_players")
        var maxPlayersPerKit: Int = 2

        @field:Configurable("games.crumble.tnt_detonation_time")
        var tntDetonationTime: Int = 80

        @field:Configurable("games.crumble.crumble_speed")
        var crumbleSpeed: Double = 0.6

        @field:Configurable("games.crumble.round_length")
        var roundLength: Int = 75
    }

    override val scoreMessages: HashMap<ScoreSource, (score: Int) -> Component> = hashMapOf(
        CommonScoreSource.TEAM_ROUND_WIN to { amount ->
            gameMessage(
                Component.text("Round Won! ", NamedTextColor.WHITE)
                    .append(Component.text("[+$amount]", NamedTextColor.GOLD))
            )
        },
        CommonScoreSource.TEAM_ROUND_DRAW to { amount ->
            gameMessage(
                Component.text("Round Drawn! ", NamedTextColor.WHITE)
                    .append(Component.text("[+$amount]", NamedTextColor.GOLD))
            )
        },
        CommonScoreSource.TEAM_ROUND_LOSE to { amount ->
            gameMessage(
                Component.text("Round Lost! ", NamedTextColor.WHITE)
                    .append(Component.text("[+$amount]", NamedTextColor.GOLD))
            )
        }
    )

    val rounds = Team.entries.filter { it.playingTeam }.size - 1
    var currentRound = 0
    val roundIndex: Int
        get() { return currentRound - 1 }

    val currentMap: LoadedMap
        get() {
            return loadedMaps[roundIndex]
        }

    var roundActive = false
    var preRound = false
    var preRoundFreeze = false

    val matchups: ArrayList<List<Pair<Team, Team>>> = ArrayList()
    val alivePlayers: HashMap<Team, ArrayList<Player>> = HashMap()
    val matchResults: ArrayList<HashMap<Team, RoundResult>> = ArrayList()

    val registeredKits: HashMap<String, Class<out Kit>> = HashMap()
    val kitTemplates: HashMap<String, Kit> = HashMap()
    val playerKits: HashMap<Player, Kit> = HashMap()
    val abilitiesUsed: ArrayList<Player> = ArrayList()

    val actionBarTasks: ArrayList<BukkitRunnable> = ArrayList()

    var currentCrumbleRadius: Double = 0.0
    var crumbleEvent: BukkitRunnable? = null

    var borderEvent: BukkitRunnable? = null

    val kitSelector: ItemStack = AdvancedItemStack(Material.COMPASS) {
        name(Component.text("Kit Selector", NamedTextColor.YELLOW))
        persistentDataContainer {
            set(kitItemsKey, PersistentDataType.BOOLEAN, true)
        }
        rightClick { player ->
            player.openHandledInventory("crumbleKitSelector")
        }
    }.build()

    val eventController: EventController by lazy {
        ControllerDelegate.getController("eventController") as EventController
    }
    val killModel = NamespacedKey("tumbling", "crumble/kill")

    override val debugToolkit = object : DebugToolkit() {
        override val events: HashMap<String, (sender: CommandSender) -> Unit> = hashMapOf(
            "selector" to { sender ->
                if(sender !is Player) {
                    sender.sendMessage(Format.error("Only players can trigger this event!"))
                    return@to
                }

                sender.inventory.addItem(kitSelector.clone())
                sender.sendMessage(Format.success("Gave the kit selector successfully!"))
            },
            "setup_warfare" to { sender ->
                if(sender !is Player) {
                    sender.sendMessage(Format.error("Only players can trigger this event!"))
                    return@to
                }

                val worldEdit = WorldEdit.getInstance()
                val sessionManager = worldEdit.sessionManager

                val playerSession = sessionManager.get(BukkitAdapter.adapt(sender))
                var clipboard: Clipboard
                try {
                    clipboard = playerSession.clipboard.clipboards.lastOrNull()
                        ?: throw IllegalStateException("No clipboard loaded for this session")
                } catch(e: Exception) {
                    sender.sendMessage(Format.error("Your worldedit clipboard is empty!"))
                    return@to
                }

                if(clipboard.region.volume == 0L) {
                    sender.sendMessage(Format.error("Your worldedit clipboard is empty!"))
                    return@to
                }

                val positions: ArrayList<BlockVector3> = arrayListOf(
                    BlockVector3.at(0, 86, 0),
                    BlockVector3.at(-500, 86, 500),
                    BlockVector3.at(500, 86, -500),
                    BlockVector3.at(-500, 86, -500),
                    BlockVector3.at(0, 86, 500),
                    BlockVector3.at(500, 86, 0)
                )

                positions.forEach {
                    clipboard.paste(BukkitAdapter.adapt(sender.world), it)
                    sender.sendMessage(Format.success("Pasted at ${it.x()}, ${it.y()}, ${it.z()} successfully!"))
                }
            },
            "kit_kill" to { sender ->
                if(sender !is Player) {
                    sender.sendMessage(Format.error("Only players can trigger this event!"))
                    return@to
                }

                val kit = playerKits[sender]
                if(kit == null) {
                    sender.sendMessage(Format.error("You do not have a kit selected!"))
                    return@to
                }

                // maybe change the `killed` field to be optional
                kit.onKill(sender)
                sender.sendMessage(Format.success("Kill event sent successfully!"))
            },
            "end_round" to { sender ->
                gameTimeoutEnd = true
                sender.sendMessage(Format.success("Ended round successfully!"))
            }
        )

        override fun killEvent(killer: Player?, killed: Player?) = playerKillAnnouncement(killer, killed)
        override fun deathEvent(killed: Player?) = playerDeathAnnouncement(killed)
    }

    override suspend fun gameLoad() {
        registerKits()

        val teams = Team.entries.filter { it.playingTeam }.toMutableList()
        teams.forEach {
            alivePlayers.put(it, arrayListOf())
        }

        repeat(rounds) {
            // thank you LichtHund for doing my job for me 👍
            val roundMatches = (0..<(teams.size/2)).map { i ->
                teams[i] to teams[teams.lastIndex - i]
            }

            matchups.add(roundMatches)
            matchResults.add(hashMapOf())

            val last = teams.removeLast()
            teams.add(1, last)
        }

        for(i in 1..rounds) {
            val map = maps.random()
            loadMap(map, i)
        }
    }

    fun registerKits() {
        registerKit("archer", ArcherKit::class.java)
        registerKit("bomber", BomberKit::class.java)
        registerKit("fisher", FisherKit::class.java)
        registerKit("hunter", HunterKit::class.java)
        registerKit("ninja", NinjaKit::class.java)
        registerKit("sorcerer", SorcererKit::class.java)
        registerKit("warrior", WarriorKit::class.java)
        registerKit("worker", WorkerKit::class.java)
    }

    fun registerKit(id: String, kit: Class<out Kit>) {
        kitTemplates.put(id, kit.getConstructor(Player::class.java, CrumbleController::class.java).newInstance(null, this))
        registeredKits.put(id, kit)
    }

    override suspend fun spawn(cycle: SpawnCycle) {
        when(cycle) {
            SpawnCycle.PREGAME -> {
                suspendSync {
                    gamePlayers.forEach {
                        spawnPlayerPregame(it)
                    }
                }
            }
            SpawnCycle.PRE_ROUND -> {
                val currentMatchups = matchups[roundIndex]
                val spawnSetKeys = (1..4).map {
                    "spawns.ingame.arena$it"
                }.toMutableList()

                currentMatchups.forEachIndexed { index, matchup ->
                    val spawns: List<List<List<Double>>> = currentMap.data
                        .getList(spawnSetKeys.getOrNull(index) ?: spawnSetKeys.first())
                        ?.map { l1 ->
                            if(l1 !is List<*>) throw GameControllerException("Spawn set is not a 2d list")
                            l1.map { l2 ->
                                if(l2 !is List<*>) throw GameControllerException("Spawn set is not a 2d list")
                                l2.map {
                                    if(it !is Double) throw GameControllerException("Spawn locations do not contain exclusively doubles")
                                    it
                                }
                            }
                        } ?: throw GameControllerException("Spawn set not found")

                    var firstOccupiedSpawns = 0
                    var secondOccupiedSpawns = 0

                    suspendSync {
                        gamePlayers.forEach {
                            it.spigot().respawn()
                            it.fireTicks = 0
                            val tumblingPlayer = it.tumblingPlayer

                            when(tumblingPlayer.team) {
                                matchup.first -> {
                                    val firstSpawnSet = spawns[0]
                                    val playerSpawn = firstSpawnSet[firstOccupiedSpawns]
                                    val location = playerSpawn.unpackCoordinates(currentMap.world)

                                    it.teleport(location)
                                    it.isFlying = false
                                    it.allowFlight = false
                                    DebugUtil.info("Spawned ${it.name} at $playerSpawn")

                                    firstOccupiedSpawns++
                                }
                                matchup.second -> {
                                    val secondSpawnSet = spawns[1]
                                    val playerSpawn = secondSpawnSet[secondOccupiedSpawns]
                                    val location = playerSpawn.unpackCoordinates(currentMap.world)

                                    it.teleport(location)
                                    DebugUtil.info("Spawned ${it.name} at $playerSpawn")

                                    secondOccupiedSpawns++
                                }
                                else -> return@forEach
                            }
                        }
                    }
                }
            }
        }
    }

    fun spawnPlayerPregame(player: Player) {
        val currentMap = loadedMaps.getOrNull(0)
        if(currentMap == null) throw GameControllerException("Current map for round $currentRound was not found")

        val pregameSpawn = currentMap.data.getList("spawns.pregame")
            ?: throw GameControllerException("Pregame spawn not specified for ${currentMap.id}")

        val location: List<Double> = pregameSpawn.map {
            if(it !is Double) throw GameControllerException("Teleport list does not contain exclusively doubles")
            it
        }

        player.teleport(location.unpackCoordinates(currentMap.world))
        DebugUtil.info("Spawned player ${player.name} at $location")
    }

    // only used for placing players into the arena during pre-round
    fun spawnPlayerPreRound(player: Player) {
        val currentMatchups = matchups[roundIndex]
        val spawnSetKeys = (1..4).map {
            "spawns.ingame.arena$it"
        }.toMutableList()

        val playerMatchup = currentMatchups.find { player in it.first.getOnlinePlayers() || player in it.second.getOnlinePlayers() }
            ?: throw GameControllerException("Could not find a valid matchup with player ${player.name}")

        val index = currentMatchups.indexOf(playerMatchup)

        val spawns: List<List<List<Double>>> = currentMap.data
            .getList(spawnSetKeys.getOrNull(index) ?: spawnSetKeys.first())
            ?.map { l1 ->
                if(l1 !is List<*>) throw GameControllerException("Spawn set is not a 2d list")
                l1.map { l2 ->
                    if(l2 !is List<*>) throw GameControllerException("Spawn set is not a 2d list")
                    l2.map {
                        if(it !is Double) throw GameControllerException("Spawn locations do not contain exclusively doubles")
                        it
                    }
                }
            } ?: throw GameControllerException("Spawn set not found")

        val team = player.tumblingPlayer.team
        val teamSpawns = if(team == playerMatchup.first) spawns[0] else spawns[1]

        // just use the first one
        player.teleport(teamSpawns[0].unpackCoordinates(currentMap.world))
    }

    fun pregamePlayer(player: Player) {
        player.inventory.addItem(kitSelector.clone())
        val task = object : BukkitRunnable() {
            override fun run() {
                var component = Component.empty()
                val kit = playerKits[player]
                if(kit != null) {
                    component = component.append(UserInterfaceUtility.backgroundTextCenter(
                        Component.text("\uEF00").font(font).shadowColor(ShadowColor.shadowColor(0)),
                        Format.mm("<icon> ${kit.name}", Placeholder.component("icon", Component.text(kit.kitIcon).font(font))),
                        kit.name,
                        69.5,
                        14.0
                    ))
                }

                player.sendActionBar(component)
            }
        }
        task.runTaskTimer(TreeTumblers.plugin, 0, 5)
        actionBarTasks.add(task)

        player.enableBossBar("countdownBossbar")
    }

    override suspend fun gamePregame() {
        gameParticipants.forEach(this::pregamePlayer)

        countdown(20, "crumble_kit_selection_timer")

        suspendSync {
            gameParticipants.forEach {
                if(!playerKits.containsKey(it)) {
                    selectKit(
                        it,
                        registeredKits.keys.filter { registeredKit ->
                            playerKits.filter { kit -> kit.value.id == registeredKit }.size < maxPlayersPerKit
                        }.random()
                    )
                }

                it.closeInventory()
                it.inventory.clear()
            }
        }
    }

    var gameTimeoutEnd = false
    override suspend fun gameOn() {
        repeat(rounds) {
            currentRound++
            gameTimeoutEnd = false
            suspendSync {
                participatingSpectators.toList().forEach(this::unSpectate)
            }
            preRound()
            suspendSync(this::giveKits)
            preRoundFreeze = true
            delay(500)
            preRoundFreeze = false
            asyncCountdown(7, "crumble_pregame_timer") {
                dropWalls()
            }
            setupCrumble()
            setupBorder()
            announceMatchup()
            preRound = false
            roundActive = true
            asyncCountdown(roundLength, "crumble_round_timer") { early ->
                if(!early) gameTimeoutEnd = true
            }
            awaitEnd()
            endRound()
        }
    }

    override suspend fun postGame() {
        val placements = getTeamPlacements()
        gameParticipants.forEach { plr ->
            val teamPlacement = placements.find { it.first == plr.tumblingPlayer.team }!!.second

            val color = when(teamPlacement) {
                1 -> NamedTextColor.GOLD
                2 -> TextColor.fromHexString("#E0E0E0")
                3 -> TextColor.fromHexString("#CE8946")
                else -> NamedTextColor.AQUA
            }

            plr.showTitle(Title.title(
                Component.text("Game Over!", NamedTextColor.RED).decorate(TextDecoration.BOLD),
                Component.text("$teamPlacement${MiscUtils.getOrdinalSuffix(teamPlacement)} place!", color),
                Title.Times.times(Tick.of(3), Tick.of(90), Tick.of(3))
            ))
            plr.sendMessage(gameMessage(Component.text("Game Over!")))
        }

        delay(5000)
        announceTeamScores()
        delay(5000)
        announceIndivScores()

        delay(5000)
        announceOverallTeamScores()
        delay(5000)
    }

    override suspend fun cleanup() {
        playerKits.forEach {
            HandlerList.unregisterAll(it.value)
        }
        actionBarTasks.forEach {
            try {
                it.cancel()
            } catch(e: Exception) {}
        }
        Bukkit.getOnlinePlayers().forEach {
            it.disableBossBar("countdownBossbar")
            it.disableBossBar("crumbleAliveTeamsBossbar")
        }
        super.cleanup()
    }

    /**
     * The method that gets called when a player joins the game during the [State.GAME_ON] state
     */
    override fun playerJoin(player: Player) {
        when(currentState) {
            State.PREGAME -> {
                spawnPlayerPregame(player)
                pregamePlayer(player)
            }
            State.GAME_ON -> {
                spawnPlayerPreRound(player)
                if(!preRound) {
                    makeSpectator(player)
                    player.sendMessage(Format.warning("You've joined while the round is active and have been placed into spectator. You will be put into the game next round."))
                }
            }
            else -> throw GameOperatorException("Game base passed playerJoin while state was neither pregame nor game on.")
        }
    }

    /**
     * The method that gets called when a player leaves the game during the [State.GAME_ON] state
     */
    override fun playerLeave(player: Player) {
        // TODO: Eliminate player, cancel game if not enough players
    }

    suspend fun preRound() {
        preRound = true
        spawn(SpawnCycle.PRE_ROUND)
        alivePlayers.values.forEach { it.clear() }
        gameParticipants.forEach { player ->
            val tumblingPlayer = player.tumblingPlayer
            alivePlayers[tumblingPlayer.team]!!.add(player)
        }
        gamePlayers.forEach {
            it.enableBossBar("crumbleAliveTeamsBossbar")
        }
        abilitiesUsed.clear()
    }

    fun setupCrumble() {
        val currentMapSize = currentMap.data.getInt("map_size")
        // basically the size is the sidelength of a square
        // so half of the diagonal is the radius to a circumscribed circle
        // and a +1 for inaccuracies in rounding
        currentCrumbleRadius = ((currentMapSize * sqrt(2.0) * 0.5) + 1)

        // This logic is human written, then optimized by AI because of how expensive these operations are
        val arenaCenters: List<Location> = (1..4).map { arena ->
            val list = currentMap.data.getList("centers.arena$arena")
                ?.map { it as? Double ?: throw GameControllerException("Center for arena $arena must be doubles") }
                ?: throw GameControllerException("Map does not have a center specified for $arena")
            list.unpackCoordinates(currentMap.world)
        }

        crumbleEvent = object : BukkitRunnable() {
            override fun run() {
                if (currentCrumbleRadius <= 0) {
                    cancel()
                    return
                }

                val radiusSquared = currentCrumbleRadius * currentCrumbleRadius

                for (center in arenaCenters) {
                    val halfMap = currentMapSize / 2
                    val xStart = (center.x - halfMap).toInt()
                    val xEnd = (center.x + halfMap).toInt()
                    val zStart = (center.z - halfMap).toInt()
                    val zEnd = (center.z + halfMap).toInt()
                    val yStart = (center.y - 50).toInt()
                    val yEnd = (center.y + 10).toInt()

                    for (x in xStart..xEnd) {
                        val dx = x + 0.5 - center.x
                        for (z in zStart..zEnd) {
                            val dz = z + 0.5 - center.z
                            val distSq = dx*dx + dz*dz
                            if (distSq < radiusSquared) continue

                            for (y in yStart..yEnd) {
                                val block = center.world.getBlockAt(x, y, z)
                                if (!block.type.isAir) {
                                    block.type = Material.AIR
                                }
                            }
                        }
                    }
                }

                currentCrumbleRadius -= crumbleSpeed / 20.0
            }
        }
        crumbleEvent!!.runTaskTimer(TreeTumblers.plugin, 0, 1)
    }

    fun setupBorder() {
        val arenaCenters: List<Location> = (1..4).map { arena ->
            val list = currentMap.data.getList("centers.arena$arena")
                ?.map { it as? Double ?: throw GameControllerException("Center for arena $arena must be doubles") }
                ?: throw GameControllerException("Map does not have a center specified for $arena")
            list.unpackCoordinates(currentMap.world)
        }

        borderEvent = object : BukkitRunnable() {
            override fun run() {
                arenaCenters.forEach { center ->
                    val points = MiscUtils.getEquidistantPoints(center, currentCrumbleRadius, 30)
                    points.forEach {
                        for(y in -5..5) {
                            currentMap.world.spawnParticle(
                                Particle.DUST,
                                it.x,
                                it.y + y,
                                it.z,
                                3,
                                Particle.DustOptions(Color.RED, 3.0f)
                            )
                        }
                    }
                }
            }
        }
        borderEvent!!.runTaskTimer(TreeTumblers.plugin, 0, 10)
    }
    
    suspend fun awaitEnd() {
        while(true) {
            val currentAlivePlayers = alivePlayers.values.sumOf { it.size }
            if(currentAlivePlayers == 0 || gameTimeoutEnd) break
            delay(200)
        }
        cancelCountdown()
    }

    suspend fun endRound() {
        roundActive = false
        crumbleEvent!!.cancel()
        crumbleEvent = null
        borderEvent!!.cancel()
        borderEvent = null
        if(gameTimeoutEnd) {
            Team.entries.filter { it.playingTeam }.forEach {
                val result = matchResults[roundIndex][it]
                if(result == null) {
                    roundDraw(it)
                }
            }
        }
        gameTimeoutEnd = false
        delay(1000)
        gamePlayers.forEach {
            it.showTitle(Title.title(
                Component.text("Round Over", NamedTextColor.RED).decoration(TextDecoration.BOLD, true),
                Component.empty(),
                Title.Times.times(Tick.of(0), Tick.of(50), Tick.of(0))
            ))
        }
        delay(2500)
    }

    suspend fun announceMatchup() {
        val roundMatchup = matchups[roundIndex]
        roundMatchup.forEach { matchup ->
            val audience = Audience.audience(matchup.first.audience, matchup.second.audience)

            val subtitle = Component.empty()
                .append(matchup.first.formattedName)
                .append(Component.text(" vs ", NamedTextColor.WHITE))
                .append(matchup.second.formattedName)
            val title = Title.title(
                Component.text("Round $currentRound", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                subtitle,
                Title.Times.times(Tick.of(3), Tick.of(80), Tick.of(3))
            )

            audience.sendMessage(gameMessage(Component.text("Round $currentRound: ", NamedTextColor.WHITE).append(subtitle)))
            audience.showTitle(title)
        }

        delay(4000)
        repeat(3) {
            roundMatchup.forEach { matchup ->
                val audience = Audience.audience(matchup.first.audience, matchup.second.audience)

                val color = when(it) {
                    0 -> NamedTextColor.GREEN
                    1 -> NamedTextColor.YELLOW
                    2 -> NamedTextColor.RED
                    else -> NamedTextColor.WHITE
                }

                val title = Title.title(
                    Component.text("Round $currentRound", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                    Component.text("> ${3 - it} <", color).decoration(TextDecoration.BOLD, true),
                    Title.Times.times(Tick.of(0), Tick.of(25), Tick.of(0))
                )


                audience.showTitle(title)
            }
            delay(1000)
        }
    }

    suspend fun dropWalls() {
        val currentMap = loadedMaps.getOrNull(roundIndex)
        if(currentMap == null) throw GameControllerException("Current map for round $currentRound was not found")

        val walls = currentMap.data.getList("walls")
            ?.map { wall ->
                if(wall !is List<*>) throw GameControllerException("Walls list is not a 2d list")
                wall.map {
                    if(it !is Int && it !is Double) throw GameControllerException("Walls list element does not contain exclusively integers or doubles.")
                    it.toDouble()
                }
            }
            ?: throw GameControllerException("Wall list not specified for ${currentMap.id}")

        walls.forEach {
            val start = it.slice(0..2).unpackCoordinates(currentMap.world)
            val end = it.slice(3..5).unpackCoordinates(currentMap.world)

            suspendSync {
                for(x in min(start.x, end.x).toInt()..max(start.x, end.x).toInt())
                for(y in min(start.y, end.y).toInt()..max(start.y, end.y).toInt())
                for(z in min(start.z, end.z).toInt()..max(start.z, end.z).toInt()) {
                    val location = Location(currentMap.world, x.toDouble(), y.toDouble(), z.toDouble())
                    location.block.type = Material.AIR

                    currentMap.world.spawnParticle(
                        Particle.BLOCK,
                        location,
                        5,
                        0.0,
                        0.0,
                        0.0,
                        location.block.blockData
                    )
                }
            }
        }

        Bukkit.broadcast(gameMessage(Component.text("Round started!")))
    }

    fun sendTeamMessage(player: Player?, message: (receiver: Player) -> Component) {
        if(player == null) {
            Bukkit.getOnlinePlayers().forEach {
                it.sendMessage(message(it))
            }
            return
        }

        val matchup = getCurrentMatchup(player)!!
        val (team1, team2) = matchup

        (team1.getOnlinePlayers() + team2.getOnlinePlayers()).forEach { it.sendMessage(message(it)) }
    }

    fun getCurrentMatchup(player: Player) = getCurrentMatchup(player.tumblingPlayer.team)

    fun getCurrentMatchup(team: Team): Pair<Team, Team>? {
        val roundMatchup = matchups[roundIndex]
        return roundMatchup.find { it.first == team || it.second == team }
    }

    fun roundWin(team: Team) {
        val title = Title.title(
            Component.text("Round Won", NamedTextColor.GREEN),
            Component.text("Well played!", NamedTextColor.WHITE),
            Title.Times.times(Tick.of(3), Tick.of(60), Tick.of(3))
        )

        team.getOnlinePlayers().forEach {
            it.showTitle(title)
        }

        grantTeamScore(team, CommonScoreSource.TEAM_ROUND_WIN)
        matchResults[roundIndex].put(team, RoundResult.WIN)
    }

    fun roundLoss(team: Team) {
        val title = Title.title(
            Component.text("Round Lost", NamedTextColor.RED),
            Component.text("Better luck next time!", NamedTextColor.WHITE),
            Title.Times.times(Tick.of(3), Tick.of(60), Tick.of(3))
        )

        team.getOnlinePlayers().forEach {
            it.showTitle(title)
        }

        grantTeamScore(team, CommonScoreSource.TEAM_ROUND_LOSE)
        matchResults[roundIndex].put(team, RoundResult.LOSS)
    }

    fun roundDraw(team: Team) {
        val title = Title.title(
            Component.text("Round Drawn", NamedTextColor.YELLOW),
            Component.empty(),
            Title.Times.times(Tick.of(3), Tick.of(60), Tick.of(3))
        )

        team.getOnlinePlayers().forEach {
            it.showTitle(title)
        }

        grantTeamScore(team, CommonScoreSource.TEAM_ROUND_DRAW)
        matchResults[roundIndex].put(team, RoundResult.DRAW)
    }

    fun playerKillAnnouncement(killer: Player?, killed: Player?) {
        sendTeamMessage(killed) {
            Format.formatKillMessage(killer, killed, it, getScoreSource(CommonScoreSource.KILL))
        }

        if(killer != null) {
            grantScore(killer, CommonScoreSource.KILL)
        }
    }

    fun playerDeathAnnouncement(killed: Player?) {
        sendTeamMessage(killed) {
            Format.formatDeathMessage(killed, it)
        }

        // Natural death in this game does not give score
    }

    fun giveKits() = playerKits.keys.forEach(this::givePlayerKit)

    fun givePlayerKit(player: Player, pregame: Boolean = false) {
        val kit = playerKits[player]!!
        kit.cleanup()
        player.inventory.clear()

        kit.items.forEach {
            val item = it.clone()
            item.itemMeta = item.itemMeta.also { meta ->
                meta.persistentDataContainer.set(kitItemsKey, PersistentDataType.BOOLEAN, true)
            }

            if(MiscUtils.isArmor(item)) {
                if(item.type.name.contains("LEATHER")) {
                    item.itemMeta = item.itemMeta.also { meta ->
                        val meta = meta as LeatherArmorMeta
                        val playerTeam = player.tumblingPlayer.team
                        meta.setColor(Color.fromRGB(playerTeam.color.value()))
                    }
                }

                player.inventory.setItem(item.type.equipmentSlot, item)
            } else {
                player.inventory.addItem(item)
            }
        }

        val abilityItem = AdvancedItemStack(Material.PAPER) {
            name(Component.text("${kit.name} Ability: ${kit.abilityName}", NamedTextColor.AQUA))
            lore(
                MiscUtils.wrapComponent(
                    Component.text(kit.abilityDescription, NamedTextColor.WHITE),
                    40
                ).toTypedArray().map { it.decoration(TextDecoration.ITALIC, false) }
            )
            model(kit.inventoryModel)
            persistentDataContainer {
                set(kitItemsKey, PersistentDataType.BOOLEAN, true)
            }

            rightClick {
                useAbility(it)
            }
        }.build()

        val killItem = ItemStack(Material.PAPER).apply {
            itemMeta = itemMeta.also { meta ->
                meta.itemName(Component.text("Kill power: ${kit.killPowerName}", NamedTextColor.YELLOW))
                meta.itemModel = killModel
                meta.persistentDataContainer.set(kitItemsKey, PersistentDataType.BOOLEAN, true)
                meta.lore(
                    MiscUtils.wrapComponent(
                        Component.text(kit.killPowerDescription, NamedTextColor.WHITE),
                        40
                    ).toTypedArray().map { it.decoration(TextDecoration.ITALIC, false) }
                )
            }
        }

        // Make sure never to have over 7 items in a kit
        player.inventory.setItem(7, killItem)
        player.inventory.setItem(8, abilityItem)

        if(pregame) {
            player.inventory.addItem(kitSelector)
        }
    }

    fun selectKit(player: Player, id: String) {
        deselectKit(player)
        require(registeredKits.get(id) != null) { "Kit with id $id does not exist" }

        val kit = registeredKits[id]!!
            .getDeclaredConstructor(Player::class.java, CrumbleController::class.java)
            .newInstance(player, this)
        playerKits.put(player, kit)
        givePlayerKit(player, true)
        Bukkit.getServer().pluginManager.registerEvents(kit, TreeTumblers.plugin)
    }

    fun deselectKit(player: Player) {
        if(!playerKits.containsKey(player)) return
        HandlerList.unregisterAll(playerKits[player]!!)
        playerKits.remove(player)
    }

    fun useAbility(player: Player) {
        if(abilitiesUsed.contains(player)) {
            player.sendMessage(Format.error("You've already used your ability!"))
            return
        }

        if(!roundActive) {
            player.sendMessage(Format.error("You cannot use your ability until the round starts!"))
            return
        }

        playerKits[player]!!.onAbility()
        player.sendMessage(Format.success("Activated ability!"))
        abilitiesUsed.add(player)
    }

    @EventHandler
    fun playerKillEvent(event: PlayerDeathEvent) {
        val killed = event.player
        val killer = killed.killer

        val tumblingPlayer = killed.tumblingPlayer
        val killedTeam = tumblingPlayer.team
        if(!killedTeam.playingTeam) return

        alivePlayers[tumblingPlayer.team]!!.remove(killed)
        val currentPlayerMatchup = getCurrentMatchup(killed)!!
        val killerTeam =
            if(currentPlayerMatchup.first == killedTeam) currentPlayerMatchup.second
            else currentPlayerMatchup.first

        if(alivePlayers[killedTeam]!!.isEmpty()) {
            // this should only really happen if they die same-tick (or if there's only one person), but im not even sure if that'd be the case
            if(alivePlayers[killerTeam]!!.isEmpty()) {
                roundDraw(killedTeam)
                roundDraw(killerTeam)
            } else {
                alivePlayers[killerTeam]!!.forEach(this::makeSpectator)
                alivePlayers[killerTeam]!!.clear()
                roundLoss(killedTeam)
                roundWin(killerTeam)
            }
        }

        if(killer == null) {
            playerDeathAnnouncement(killed)
            return
        }

        playerKillAnnouncement(killer, killed)
    }

    // Some maps have the spawn point right next to lava, so if you move immediately after spawning you'd just run right in
    @EventHandler
    fun playerMoveEvent(event: PlayerMoveEvent) {
        if(
            preRoundFreeze
            && (event.to.x != event.from.x || event.to.z != event.from.z)
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun playerVoidEvent(event: PlayerMoveEvent) {
        if(!roundActive || participatingSpectators.contains(event.player)) return

        val voidHeight = currentMap.data.getInt("kill_height")
        if(event.to.y < voidHeight) {
            event.player.health = 0.0
        }
    }

    @EventHandler
    fun playerDropItemEvent(event: PlayerDropItemEvent) {
        val item = event.itemDrop
        if(item.itemStack.persistentDataContainer.get(kitItemsKey, PersistentDataType.BOOLEAN) == true) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun playerTntEvent(event: BlockPlaceEvent) {
        if(!roundActive) {
            event.isCancelled = true
            return
        }

        if(flags.contains(Flag.DISABLE_TNT_AUTO_EXPLODE)) return

        val player = event.player
        val block = event.block
        if(block.type != Material.TNT) return

        val item = event.itemInHand

        val location = block.location
        location.block.type = Material.AIR

        val tnt = location.world.spawnEntity(location, EntityType.TNT) as TNTPrimed
        tnt.persistentDataContainer.set(
            NamespacedKey("tumbling", "tnt_owner"),
            PersistentDataType.STRING,
            player.uniqueId.toString()
        )

        if(item.itemMeta?.persistentDataContainer?.get(BomberKit.nukeKey, PersistentDataType.BOOLEAN) == true) {
            // maybe un-hardcode this
            tnt.fuseTicks = BomberKit.nukeExplosionTicks
            tnt.persistentDataContainer.set(
                BomberKit.nukeKey,
                PersistentDataType.BOOLEAN,
                true
            )
        } else {
            tnt.fuseTicks = tntDetonationTime
        }
    }

    @EventHandler
    fun playerDamageEvent(event: EntityDamageEvent) {
        if(event.entity !is Player) return
        if(!roundActive) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun tntDamageEvent(event: EntityDamageEvent) {
        val player = event.entity
        if(player !is Player || event.isCancelled) return

        val causingEntity = event.damageSource.directEntity
        if(causingEntity == null || causingEntity !is TNTPrimed) return

        val dataContainer = causingEntity.persistentDataContainer
        val causingPlayerUUID = dataContainer.get(NamespacedKey("tumbling", "tnt_owner"), PersistentDataType.STRING)
        val causingPlayer = Bukkit.getPlayer(UUID.fromString(causingPlayerUUID))

        if(causingPlayerUUID == null || causingPlayer == null) {
            DebugUtil.severe("Could not find a causing player on an exploding tnt")
            event.isCancelled = true
            return
        }

        val causingTeam = causingPlayer.tumblingPlayer.team
        if(causingTeam.getOnlinePlayers().contains(player) && player != causingPlayer) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun playerHitEvent(event: EntityDamageByEntityEvent) {
        val damaged = event.entity
        val damager = event.damager

        if(damaged !is Player || damager !is Player) return

        if(damager.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            damager.removePotionEffect(PotionEffectType.BLINDNESS)
        }
    }

    @EventHandler
    fun preRoundBlockBreakEvent(event: BlockBreakEvent) {
        if(!roundActive) event.isCancelled = true
    }

    enum class RoundResult {
        WIN,
        LOSS,
        DRAW
    }
}