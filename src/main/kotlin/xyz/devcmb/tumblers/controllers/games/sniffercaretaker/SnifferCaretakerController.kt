package xyz.devcmb.tumblers.controllers.games.sniffercaretaker


import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.regions.CuboidRegion
import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Chest
import org.bukkit.block.data.Ageable
import org.bukkit.block.data.Levelled
import org.bukkit.block.data.type.TrapDoor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Sniffer
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExpEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.tasks.BoredTask
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.tasks.HungryTask
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.tasks.LonelyTask
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.tasks.ThirstyTask
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.DebugToolkit
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.engine.score.ScoreSource
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.enableBossBar
import xyz.devcmb.tumblers.util.fill
import xyz.devcmb.tumblers.util.randomBetween
import xyz.devcmb.tumblers.util.runTaskLater
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.validateLocation
import kotlin.collections.forEach
import kotlin.math.max
import kotlin.math.min

@EventGame
class SnifferCaretakerController : GameBase(
    id = "snifferCaretaker",
    votable = true,
    maps = setOf(
        Map("facility")
    ),
    cutsceneSteps = arrayListOf(
        CutsceneStep(
            Component.empty()
                .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
                .append(Component.text("\uEA00").font(NamespacedKey("tumbling", "games/sniffer_caretaker")))
                .append(Component.text(" Sniffer Caretaker"))
        ) { map ->
            teleportConfig("cutscene.start")
            delay(5000)
        },
        CutsceneStep(Format.mm("In this game, you need to fulfill your <red>sniffer's</red> wants as seen on the <blue>task board.</blue> <blue>Tasks</blue> will give more <yellow>score</yellow> based on how many <aqua>stars</aqua> it has.")
        ) { map ->
            val game = game as SnifferCaretakerController
            teleportConfig("cutscene.tasks")

            val exampleTasks: List<String> = listOf(
                "hungry_wheat",
                "bored_moss_block",
                "lonely_cow",
                "hungry_pumpkin_pie",
                "hungry_cake"
            )

            exampleTasks.forEach {
                suspendSync {
                    game.createNewTask(Team.RED, it, true)
                }
                delay(750)
            }

            delay(3000)
        },
        CutsceneStep(Format.mm("<blue>Tasks</blue> range from feeding the sniffer various foods...")
        ) { map ->
            val game = game as SnifferCaretakerController

            suspendSync {
                game.currentTasks[Team.RED]!!.forEach {
                    game.completeTask(Team.RED, it, true)
                }

                game.currentTasks[Team.RED]!!.clear()
            }

            teleportConfig("cutscene.farm")

            map.data.getList("cutscene.farm_wheat")?.forEach {
                if(it !is List<*>) throw GameControllerException("Cutscene farm wheat location table is not a list")
                val location = it.validateLocation(map.world) ?: throw GameControllerException("Cutscene farm wheat locations are not valid")

                suspendSync {
                    location.block.type = Material.WHEAT
                    map.world.playSound(location, Sound.ITEM_CROP_PLANT, 1.0f, 1.0f)
                }

                delay(100)
            }

            delay(1500)

            suspendSync {
                map.data.getList("cutscene.farm_wheat")?.forEach {
                    if (it !is List<*>) throw GameControllerException("Cutscene farm wheat location table is not a list")
                    val location = it.validateLocation(map.world)
                        ?: throw GameControllerException("Cutscene farm wheat locations are not valid")

                    location.block.type = Material.AIR
                }
            }
        },
        CutsceneStep(Format.mm("To giving the sniffer things to sniff...")
        ) { map ->
            val game = game as SnifferCaretakerController

            suspendSync {
                game.stockBlocks(Team.RED)
            }

            teleportConfig("cutscene.blocks")
            delay(2000)
        },
        CutsceneStep(Format.mm("To quenching the sniffer's thirst...")
        ) { map ->
            teleportConfig("cutscene.thirst")
            delay(500)

            val cauldron = getLocation("cutscene.thirst_cauldron")
            suspendSync {
                cauldron.block.type = Material.WATER_CAULDRON
                cauldron.block.blockData = (cauldron.block.blockData as Levelled).also {
                    it.level = it.maximumLevel
                }
                map.world.playSound(cauldron, Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.0f)
            }

            delay(1500)

            suspendSync {
                cauldron.block.type = Material.CAULDRON
            }
        },
        CutsceneStep(Format.mm("To bringing it a friend!")
        ) { map ->
            teleportConfig("cutscene.mobs")

            val cowLocation = getLocation("cutscene.mobs_cow")
            val chickenLocation = getLocation("cutscene.mobs_chicken")
            lateinit var cow: Entity
            lateinit var chicken: Entity

            suspendSync {
                cow = map.world.spawnEntity(cowLocation, EntityType.COW)
                chicken = map.world.spawnEntity(chickenLocation, EntityType.CHICKEN)
            }

            delay(2500)

            suspendSync {
                cow.remove()
                chicken.remove()
            }
        },
        CutsceneStep(Format.mm("The <red>sniffer</red> may also have some rather <i>odd</i> desires, which can be found in the back of the facility in the <red>basement...</red>")
        ) { map ->
            teleportConfig("cutscene.basement")

            val spiderLocation = getLocation("cutscene.basement_spider")
            lateinit var spider: Entity

            suspendSync {
                spider = map.world.spawnEntity(spiderLocation, EntityType.SPIDER)
            }

            delay(5000)

            suspendSync {
                spider.remove()
            }
        },
        CutsceneStep(Format.mm("Remember, your only goal is to keep the <red>sniffer</red> <yellow>happy</yellow>, and complete as many <blue>tasks</blue> as you can!")
        ) { map ->
            teleportConfig("cutscene.end")
            delay(4000)
        },
        CutsceneStep(Format.mm("<b><green>Good Luck, Have Fun!</green></b>")
        ) { map ->
        }
    ),
    flags = emptySet(),
    scores = hashMapOf(
        SnifferCaretakerScoreSource.TASK_1_STAR to 20,
        SnifferCaretakerScoreSource.TASK_2_STAR to 40,
        SnifferCaretakerScoreSource.TASK_3_STAR to 60,
        SnifferCaretakerScoreSource.TASK_4_STAR to 80,
        SnifferCaretakerScoreSource.TASK_5_STAR to 120
    ),
    icon = Component.text("\uEA00").font(NamespacedKey("tumbling", "games/sniffer_caretaker")),
    scoreboard = "snifferCaretakerScoreboard",
    name = "Sniffer Caretaker"
) {
    companion object {
        val snifferTeamKey = NamespacedKey("tumbling", "sniffer_team")

        @field:Configurable("games.snifferCaretaker.game_length")
        var gameLength: Int = 600

        @field:Configurable("games.snifferCaretaker.chest_refresh")
        var chestRefresh: Long = 20

        @field:Configurable("games.snifferCaretaker.block_refresh")
        var blockRefresh: Long = 15

        @field:Configurable("games.snifferCaretaker.mob_refresh")
        var mobRefresh: Long = 30

        @field:Configurable("games.snifferCaretaker.task_interval")
        var taskInterval: Long = 8
    }

    override val scoreMessages: HashMap<ScoreSource, (score: Int) -> Component> = hashMapOf(
        SnifferCaretakerScoreSource.TASK_1_STAR to { amount -> gameMessage(Format.mm("Completed a <aqua>1-Star Task!</aqua> <gold>[+$amount]</gold>")) },
        SnifferCaretakerScoreSource.TASK_2_STAR to { amount -> gameMessage(Format.mm("Completed a <dark_purple>2-Star Task!</dark_purple> <gold>[+$amount]</gold>")) },
        SnifferCaretakerScoreSource.TASK_3_STAR to { amount -> gameMessage(Format.mm("Completed a <color:#ff9100>3-Star Task!</color> <gold>[+$amount]</gold>")) },
        SnifferCaretakerScoreSource.TASK_4_STAR to { amount -> gameMessage(Format.mm("Completed a <red>4-Star Task!</red> <gold>[+$amount]</gold>")) },
        SnifferCaretakerScoreSource.TASK_5_STAR to { amount -> gameMessage(Format.mm("Completed a <yellow>5-Star Task!</yellow> <gold>[+$amount]</gold>")) },
    )

    override val debugToolkit = object : DebugToolkit() {
        override val events: HashMap<String, (sender: CommandSender) -> Unit> = hashMapOf(
            "create_task" to { sender ->
                if (sender !is Player) {
                    sender.sendMessage(Format.error("Only players can trigger this event!"))
                    return@to
                }

                Team.entries.filter { it.playingTeam }.forEach {
                    createNewTask(it, taskQueue.random())
                }

                sender.sendMessage(Format.success("Created a new task!"))
            },
            "setup_facility" to { sender ->
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

                val region = CuboidRegion(clipboard.region.minimumPoint, clipboard.region.maximumPoint)

                repeat(7) {
                    val forwardExtentCopy = ForwardExtentCopy(playerSession.selectionWorld, region, playerSession.selectionWorld, region.minimumPoint.add((it + 1) * 1000, 0, 0))
                    Operations.complete(forwardExtentCopy)
                    sender.sendMessage(Format.success("Pasted $it successfully!"))
                }
            },
        )

        override fun killEvent(killer: Player?, killed: Player?) {}
        override fun deathEvent(killed: Player?) {}
    }

    val currentMap: LoadedMap
        get() {
            return loadedMaps[0]
        }

    val breakableBlocks: List<Material> = listOf(
        Material.WHEAT_SEEDS,
        Material.WHEAT,
        Material.DIRT,
        Material.MOSS_BLOCK,
        Material.MUD,
        Material.GRAVEL,
        Material.SAND,
        Material.COARSE_DIRT,
        Material.PUMPKIN,
        Material.PUMPKIN_STEM,
        Material.BROWN_MUSHROOM,
        Material.RED_MUSHROOM,
        Material.LADDER, // because you can craft them, and im not stopping you!
        Material.GLASS
    )

    val kit: List<ItemStack> = listOf(
        ItemStack(Material.WOODEN_SWORD).apply {
            itemMeta = itemMeta.also {
                it.isUnbreakable = true
            }
        },

        ItemStack(Material.STONE_SHOVEL).apply {
            itemMeta = itemMeta.also {
                it.isUnbreakable = true
            }
        },

        ItemStack(Material.BONE_MEAL, 64),
        ItemStack(Material.BUCKET),
        ItemStack(Material.GLASS_BOTTLE),
        ItemStack(Material.BOWL)
    )

    val chestItems: HashMap<String, List<Pair<Material, Int>>> = hashMapOf(
        "supply_chest" to listOf(
            Pair(Material.WHEAT_SEEDS, 6),
            Pair(Material.PUMPKIN_SEEDS, 1),
            Pair(Material.SUGAR_CANE, 1)
        ),
        "basement_supply_chest" to listOf(
            Pair(Material.RED_MUSHROOM, 3),
            Pair(Material.BROWN_MUSHROOM, 3),
            Pair(Material.STICK, 5),
        )
    )

    val blockItems: HashMap<String, List<List<*>>> = hashMapOf(
        "dirt" to listOf(
            listOf(Material.DIRT, 4),
            listOf(Material.SAND, 4),
            listOf(Material.GRAVEL, 4)
        ),
        "moss" to listOf(
            listOf(Material.STONE, 6),
            listOf(Material.MOSS_BLOCK, 2)
        )
    )

    val mobSpawns: List<EntityType> = listOf(
        EntityType.CHICKEN,
        EntityType.COW
    )

    var timers = mutableMapOf(
        "supply_chest" to chestRefresh * 20,
        "basement_supply_chest" to chestRefresh * 20,
        "dirt" to blockRefresh * 20,
        "moss" to blockRefresh * 20,
        "mob" to mobRefresh * 20
    )

    val timerBases = hashMapOf(
        "supply_chest" to chestRefresh * 20,
        "basement_supply_chest" to chestRefresh * 20,
        "dirt" to blockRefresh * 20,
        "moss" to blockRefresh * 20,
        "mob" to mobRefresh * 20
    )

    val taskQueue: MutableList<String> = mutableListOf()
    val taskIndexes: HashMap<Team, Int> = hashMapOf()
    val currentTasks: HashMap<Team, MutableList<Task>> = hashMapOf()

    val signs: HashMap<Team, HashMap<String, TextDisplay>> = hashMapOf()
    val sniffers: HashMap<Team, Sniffer> = hashMapOf()
    val spawnedMobs: HashMap<Team, MutableList<Entity>> = hashMapOf()

    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    override suspend fun gameLoad() {
        val map = loadMap(maps.random(), 1)

        var smallestTeam = 9999

        Team.entries.forEach {
            if (it.playingTeam) {
                smallestTeam = min(smallestTeam, it.getOnlinePlayers().size)
            }
        }

        val tasks: List<String> = TreeTumblers.plugin.config.getList("games.snifferCaretaker.tasks")?.map {
            if (
                it !is HashMap<*, *>
                || it["id"] == null
                || it["id"] !is String
                || it["stars"] == null
                || it["stars"] !is Int
                || it["item"] == null
                || it["item"] !is String
            ) throw GameControllerException("Task contains invalid data")
            it["id"] as String
        } ?: throw GameControllerException("Task list not found")

        fun pickTask(): String {
            val chosen = tasks.random()

            // removes the cake task from the pool if any team doesn't have at least 3 players
            // bc you need 3 players and 3 buckets to make a cake..!
            if (smallestTeam < 3 && chosen == "hungry_cake") return pickTask()
            if (taskQueue.isEmpty()) return chosen
            val min = max(0, taskQueue.size - 6)
            val max = taskQueue.size - 1

            for (i in min..max) {
                if (taskQueue[i] == chosen) return pickTask()
            }

            return chosen
        }

        repeat(100) {
            taskQueue.add(pickTask())
        }

        suspendSync {
            Team.entries.filter { it.playingTeam }.forEach {
                taskIndexes[it] = 0
                currentTasks[it] = mutableListOf()
                signs[it] = hashMapOf()
                spawnedMobs[it] = mutableListOf()

                val snifferSpawn = map.data.getList("sniffer_spawn")?.validateLocation(map.world)
                    ?: throw GameControllerException("Sniffer spawns not found")

                val snifferLocation = offsetLocation(snifferSpawn, it)

                snifferLocation.chunk.load()

                chestItems.forEach { key, _ ->
                    val chestPosition = currentMap.data.getList(key)?.validateLocation(map.world)
                        ?: throw GameControllerException("Chest position not found")

                    val chestLocation = offsetLocation(chestPosition, it)
                    chestLocation.chunk.load()
                }

                val sniffer = map.world.spawn(snifferLocation, Sniffer::class.java) { mob ->
                    mob.isInvulnerable = true
                    mob.persistentDataContainer.set(
                        snifferTeamKey,
                        PersistentDataType.STRING,
                        it.name
                    )
                    mob.setAI(false) // for cutscenes sake
                }

                sniffers[it] = sniffer
            }
        }
    }

    override suspend fun gamePregame() {
        spawn(SpawnCycle.PRE_ROUND)

        gamePlayers.forEach {
            it.enableBossBar("countdownBossbar")
        }
        asyncCountdown(7)

        delay(2000)

        MiscUtils.titleCountdown(Audience.audience(gamePlayers), Format.mm("Game starts in"), 5)
    }

    /**
     * The abstract method for spawning players in
     *
     * There was going to be some kind of system to do this automatically, but doing it manually seems to be a more flexible option, at least for now.
     *
     * @param cycle The stage where the players are spawned
     */
    override suspend fun spawn(cycle: SpawnCycle) {
        if (cycle != SpawnCycle.PRE_ROUND) return
        val map = loadedMaps[0]

        suspendSync {
            gamePlayers.forEach {
                it.spigot().respawn()
                it.gameMode = GameMode.SURVIVAL
                val tumblingPlayer = it.tumblingPlayer

                val playerSpawn = currentMap.data.getList("spawn")?.validateLocation(map.world)
                    ?: throw GameControllerException("Spawn not found")

                val playerLocation = offsetLocation(playerSpawn, tumblingPlayer.team)

                it.teleport(playerLocation)

                kit.forEach { item ->
                    it.inventory.addItem(item)
                }
            }
        }
    }

    /**
     * The method for the main gameplay loop for an individual game
     *
     * This should contain any kind of game-specific logic, and round handling if applicable
     */
    override suspend fun gameOn() {
        val chestTask = object : BukkitRunnable() {
            override fun run() {
                Team.entries.filter { it.playingTeam }.forEach {
                    stockChests(it)
                }
            }
        }

        val blockTask = object : BukkitRunnable() {
            override fun run() {
                Team.entries.filter { it.playingTeam }.forEach {
                    stockBlocks(it)
                }
            }
        }

        val blockContainmentClosingTask = object : BukkitRunnable() {
            override fun run() {
                Team.entries.filter { it.playingTeam }.forEach {
                    closeBlockContainments(it)
                }
            }
        }

        val mobTask = object : BukkitRunnable() {
            override fun run() {
                Team.entries.filter { it.playingTeam }.forEach {
                    stockMobs(it)
                }
            }
        }

        val tickTask = object : BukkitRunnable() {
            override fun run() {
                timers.forEach { key, it ->
                    timers[key] = timers[key]!! - 1
                    if (timers[key]!! < 0) {
                        timers[key] = timerBases[key]!!
                    }
                }

                Team.entries.filter { it.playingTeam }.forEach { team ->
                    updateSigns(team)
                    sniffers[team]!!.setAI(true) // its beiung stupid aand im mad
                }
            }
        }

        val taskTask = object : BukkitRunnable() { // don't you just love running tasks to create tasks which requires tasks to finish the tasks :D
            override fun run() {
                Team.entries.filter { it.playingTeam }.forEach {
                    if (currentTasks[it]!!.size <= 5) {
                        createNewTask(it, taskQueue[taskIndexes[it]!!])
                        taskIndexes[it] = taskIndexes[it]!! + 1
                    }
                }
            }
        }

        suspendSync {
            Team.entries.filter { it.playingTeam }.forEach {
                setupSigns(it)
                sniffers[it]!!.setAI(true)
            }

            chestTask.runTaskTimer(TreeTumblers.plugin, 0, 20*chestRefresh)
            blockTask.runTaskTimer(TreeTumblers.plugin, 0, 20*blockRefresh)
            blockContainmentClosingTask.runTaskTimer(TreeTumblers.plugin, 20*(blockRefresh-6), 20*blockRefresh)
            mobTask.runTaskTimer(TreeTumblers.plugin, 0, 20*mobRefresh)
            tickTask.runTaskTimer(TreeTumblers.plugin, 1, 1)
            taskTask.runTaskTimer(TreeTumblers.plugin, 0, 20*taskInterval)
        }

        countdown(gameLength)

        suspendSync {
            chestTask.cancel()
            blockTask.cancel()
            blockContainmentClosingTask.cancel()
            mobTask.cancel()
            tickTask.cancel()
            taskTask.cancel()
        }
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {
        suspendSync {
            currentTasks.forEach { team, tasks ->
                tasks.forEach { task ->
                    completeTask(
                        team,
                        task,
                        true
                    ) // the value may be "is cutscene step" but it does already destroy the task without giving points. Yay!
                }
            }
        }

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

    fun offsetLocation(location: Location, team: Team): Location {
        return location.clone().add((team.priority - 1) * 1000.0, 0.0, 0.0)
    }

    fun stockChests(team: Team) {
        chestItems.forEach { key, it ->
            val chestPosition = currentMap.data.getList(key)?.validateLocation(currentMap.world)
                ?: throw GameControllerException("Chest position not found")

            val chestLocation = offsetLocation(chestPosition, team)
            currentMap.world.getBlockAt(chestLocation).type = Material.CHEST

            val chest = chestLocation.block.state as Chest
            val inventory = chest.inventory

            inventory.clear()

            it.forEach { item ->
                val material = item.first
                val amount = item.second

                fun chooseIndex() : Int {
                    val index = (0..26).random()
                    val slot = inventory.getItem(index)

                    if (slot == null) return index
                    if (slot.type != material) return chooseIndex()

                    return index
                }

                repeat(amount) {
                    val index = chooseIndex()
                    val slot = inventory.getItem(index)

                    if (slot == null) {
                        inventory.setItem(index, ItemStack(material))
                    } else {
                        slot.add(1)
                    }
                }
            }
        }
    }

    fun stockBlocks(team: Team) {
        blockItems.forEach { (key, it) ->
            val floor = currentMap.data.getList("block_containments.$key.floor")?.map { it as List<*>
                it.validateLocation(currentMap.world)
            } ?: throw GameControllerException("Floor for block containment $key must be defined")

            val area = currentMap.data.getList("block_containments.$key.area")?.map { it as List<*>
                it.validateLocation(currentMap.world)
            } ?: throw GameControllerException("Area for block containment $key must be defined")

            val door = currentMap.data.getList("block_containments.$key.door")?.map { it as List<*>
                it.validateLocation(currentMap.world)
            } ?: throw GameControllerException("Door for block containment $key must be defined")

            val floorMin = offsetLocation(floor[0]!!, team)
            val floorMax = offsetLocation(floor[1]!!, team)

            val areaMin = offsetLocation(area[0]!!, team)
            val areaMax = offsetLocation(area[1]!!, team)

            val doorMin = offsetLocation(door[0]!!, team)
            val doorMax = offsetLocation(door[1]!!, team)

            var i = 0

            for (it in (doorMin.y.toInt()..doorMax.y.toInt())) {
                runTaskLater((i * 3).toLong()) {
                    currentMap.world.fill(
                        Location(currentMap.world, doorMin.x, it.toDouble(), doorMin.z),
                        Location(currentMap.world, doorMax.x, it.toDouble(), doorMax.z),
                        Material.AIR
                    )
                }

                i++
            }

            currentMap.world.fill(
                floorMin,
                floorMax,
                Material.SMOOTH_STONE
            )

            currentMap.world.fill(areaMin, areaMax, Material.AIR)

            it.forEach { value ->
                val material = value[0] as Material
                val amount = value[1] as Int

                fun choosePosition(): Location {
                    val pos = areaMin.randomBetween(areaMax)
                    val block = pos.block
                    if (block.type != material && block.type != Material.AIR) return choosePosition()

                    return pos
                }

                repeat(amount) {
                    val pos = choosePosition()
                    pos.block.type = material
                }
            }
        }
    }

    fun closeBlockContainments(team: Team) {
        blockItems.forEach { (key, _) ->
            val floor = currentMap.data.getList("block_containments.$key.floor")?.map { it as List<*>
                it.validateLocation(currentMap.world)
            } ?: throw GameControllerException("Floor for block containment $key must be defined")

            val area = currentMap.data.getList("block_containments.$key.area")?.map { it as List<*>
                it.validateLocation(currentMap.world)
            } ?: throw GameControllerException("Area for block containment $key must be defined")

            val door = currentMap.data.getList("block_containments.$key.door")?.map { it as List<*>
                it.validateLocation(currentMap.world)
            } ?: throw GameControllerException("Door for block containment $key must be defined")

            val floorMin = offsetLocation(floor[0]!!, team)
            val floorMax = offsetLocation(floor[1]!!, team)

            val areaMin = offsetLocation(area[0]!!, team)
            val areaMax = offsetLocation(area[1]!!, team)

            val doorMin = offsetLocation(door[0]!!, team)
            val doorMax = offsetLocation(door[1]!!, team)

            for (i in (0..6)) {
                runTaskLater((i * 10).toLong()) {
                    currentMap.world.fill(
                        floorMin,
                        floorMax,
                        if (i % 2 == 0) Material.LAPIS_ORE else Material.SMOOTH_STONE
                    )
                }
            }

            var i = 0

            for (it in (doorMin.y.toInt()..doorMax.y.toInt()).reversed()) {
                runTaskLater((i * 3).toLong() + 70) {
                    currentMap.world.fill(
                        Location(currentMap.world, doorMin.x, it.toDouble(), doorMin.z),
                        Location(currentMap.world, doorMax.x, it.toDouble(), doorMax.z),
                        Material.WAXED_OXIDIZED_COPPER_GRATE
                    )
                }

                i++
            }

            runTaskLater(70) {
                currentMap.world.fill(floorMin, floorMax, Material.AIR)
                currentMap.world.fill(areaMin, areaMax, Material.AIR)
            }
        }
    }

    fun stockMobs(team: Team) {
        val mobCoordinates = currentMap.data.getList("mob_spawn")?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Mob spawn not found")

        val mobLocation = offsetLocation(mobCoordinates, team)

        mobSpawns.forEach {
            val entity = currentMap.world.spawnEntity(mobLocation, it)
            spawnedMobs[team]!!.add(entity)
        }


        val spiderMobCoordinates = currentMap.data.getList("spider_spawn")?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Spider spawn not found")

        val spiderMobLocation = offsetLocation(spiderMobCoordinates, team)

        currentMap.world.spawnEntity(spiderMobLocation, EntityType.SPIDER)
    }

    fun completeTask(team: Team, task: Task, isCutsceneStep: Boolean = false) {
        if (isCutsceneStep) {
            task.display?.remove()
            task.destroy()
            HandlerList.unregisterAll(task)
            return
        }

        task.count -= 1

        if (task.count > 0) return

        val sniffer = sniffers[team]!!

        currentMap.world.playSound(sniffer.location, Sound.ENTITY_SNIFFER_HAPPY, 1.0f, 1.0f)
        currentMap.world.spawnParticle(Particle.HEART, sniffer.location.add(0.0,2.0,0.0), 5, 0.1,0.1,0.1)

        if (task.completer != null) {
            grantScore(task.completer!!, SnifferCaretakerScoreSource.valueOf("TASK_${task.stars}_STAR"))
        } else {
            grantTeamScore(team, SnifferCaretakerScoreSource.valueOf("TASK_${task.stars}_STAR"))
        }

        task.display?.remove()
        task.destroy()
        HandlerList.unregisterAll(task)

        currentTasks[team]!!.remove(task)

        val displaySpawn = currentMap.data.getList("task_board")?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Task board spawn not found")

        currentTasks[team]!!.forEachIndexed { index, it ->
            val displayLocation = offsetLocation(displaySpawn, team).add(0.0, index * 0.5, 0.0)
            it.display?.teleport(displayLocation)
        }
    }

    fun taskDisplayTextStars(task: Task): Component {
        var text = Format.mm(task.displayText)

        val starSprite = when(task.stars) {
            1 -> "\uEF00"
            2 -> "\uEF01"
            3 -> "\uEF02"
            4 -> "\uEF03"
            5 -> "\uEF04"
            else -> "\uEF00"
        }

        text = text.append(Component.text(" "))

        for (i in (1..task.stars)) {
            text = text.append(Component.text(starSprite).font(NamespacedKey("tumbling", "games/sniffer_caretaker")))
        }

        val score = scores.get(SnifferCaretakerScoreSource.valueOf("TASK_${task.stars}_STAR"))

        text = text.append(Format.mm(" <gold>[+${score}]</gold>"))

        return text
    }

    fun createNewTask(team: Team, task: String, isCutsceneStep: Boolean = false) {
        val tasks: List<HashMap<*, *>> = TreeTumblers.plugin.config.getList("games.snifferCaretaker.tasks")?.map {
            if (
                it !is HashMap<*, *>
                || it["id"] == null
                || it["id"] !is String
                || it["stars"] == null
                || it["stars"] !is Int
                || it["item"] == null
                || it["item"] !is String
            ) throw GameControllerException("Task contains invalid data")
            it
        } ?: throw GameControllerException("Task list not found")

        val chosenTask: HashMap<*, *> = (tasks.find {
            it["id"] == task
        } ?: throw GameControllerException("Invalid task ID"))

        val stars = chosenTask["stars"]
        if (stars !is Int) return

        val id = chosenTask["id"]
        if (id !is String) return

        val item = chosenTask["item"]
        if (item !is String) return

        DebugUtil.info("Creating Task $id : Stars $stars Item $item")

        val createdTask = when (chosenTask["type"]) {
            "HUNGRY" -> HungryTask(
                team,
                id,
                this,
                stars,
                Material.getMaterial(item)
            )
            "THIRSTY" -> ThirstyTask(
                team,
                id,
                this,
                stars,
                Material.getMaterial(item)
            )
            "BORED" -> BoredTask(
                team,
                id,
                this,
                stars,
                Material.getMaterial(item)
            )
            "LONELY" -> LonelyTask(
                team,
                id,
                this,
                stars,
                EntityType.valueOf(item)
            )
            else -> throw GameControllerException("Task type invalid")
        }

        val displaySpawn = currentMap.data.getList("task_board")?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Task board spawn not found")

        val displayLocation = offsetLocation(displaySpawn, team).add(0.0, currentTasks[team]!!.size * 0.5, 0.0)

        val display: TextDisplay = currentMap.world.spawn(displayLocation, TextDisplay::class.java) {
            it.text(taskDisplayTextStars(createdTask))
            it.alignment = TextDisplay.TextAlignment.LEFT
            it.lineWidth = 400
        }

        createdTask.display = display
        createdTask.init()

        currentTasks.get(team)!!.add(createdTask)
        currentMap.world.playSound(displayLocation, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, (createdTask.stars.toFloat() / 12f) + 0.7f)
        Bukkit.getServer().pluginManager.registerEvents(createdTask, TreeTumblers.plugin)

        if (!isCutsceneStep) {
            team.getOnlinePlayers().forEach { player ->
                player.sendMessage(taskDisplayTextStars(createdTask))
            }
        }
    }

    fun setupSign(key: String, team: Team) : TextDisplay {
        val displaySpawn = currentMap.data.getList(key)?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Sign spawn not found at $key")

        val displayLocation = offsetLocation(displaySpawn, team)

        val display: TextDisplay = currentMap.world.spawn(displayLocation, TextDisplay::class.java) {
            it.alignment = TextDisplay.TextAlignment.CENTER
            it.lineWidth = 400
            it.isPersistent = true
            it.text(Format.mm("loading..."))
        }

        return display
    }

    fun setupSigns(team: Team) {
        signs[team]!!["supply_chest"] = setupSign("supply_chest_sign", team)
        signs[team]!!["basement_supply_chest"] = setupSign("basement_supply_chest_sign", team)
        signs[team]!!["dirt"] = setupSign("block_containments.dirt.sign", team)
        signs[team]!!["moss"] = setupSign("block_containments.moss.sign", team)
        signs[team]!!["mob"] = setupSign("mob_spawn_sign", team)
    }

    fun updateSigns(team: Team) {
        signs[team]!!.forEach { key, it ->
            it.interpolationDelay = 0
            it.interpolationDuration = 0
            it.text(Component.text("Restocking in ${MiscUtils.formatToMSS(timers[key]!!.toInt() / 20)}"))
        }

        currentTasks[team]!!.forEach {
            it.display?.text(taskDisplayTextStars(it))
        }
    }

    @EventHandler
    fun blockBreakEventSnifferCaretaker(event: BlockBreakEvent) {
        if (!breakableBlocks.contains(event.block.type)) {
            event.isCancelled = true
        }

        if (event.block.type == Material.WHEAT) {
            val ageable = event.block.blockData as Ageable
            if (ageable.age < ageable.maximumAge) { return }

            event.isCancelled = true
            currentMap.world.getBlockAt(event.block.location).type = Material.AIR
            currentMap.world.dropItem(event.block.location, ItemStack(Material.WHEAT))
        }
    }

    @EventHandler
    fun playerInteractEvent(event: PlayerInteractEvent) {
        // infinite bone meal
        if (event.action == Action.RIGHT_CLICK_BLOCK && event.item?.type == Material.BONE_MEAL && event.clickedBlock?.blockData is Ageable) {
            event.isCancelled = true
            event.clickedBlock?.applyBoneMeal(event.blockFace)
        }

        // prevent opening trapdoors !!
        if (event.action == Action.RIGHT_CLICK_BLOCK && event.clickedBlock?.blockData is TrapDoor) {
            event.isCancelled = true
        }

        // prevent farmland trampling!
        if (event.action == Action.PHYSICAL && event.clickedBlock?.type == Material.FARMLAND) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun playerBucketFillEvent(event: PlayerBucketFillEvent) {
        event.isCancelled = true
        event.player.inventory.setItem(event.hand, ItemStack(Material.WATER_BUCKET))
    }

    @EventHandler
    fun playerBucketEmptyEvent(event: PlayerBucketEmptyEvent) {
        event.isCancelled = true // prevents you from spilling water everywhere and being evil which is bad and evil
    }

    @EventHandler
    fun playerInteractEntityEvent(event: PlayerInteractEntityEvent) {
        if (event.rightClicked.type == EntityType.COW || event.rightClicked.type == EntityType.CHICKEN) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun craftItemEvent(event: CraftItemEvent) {
        // prevent the bone meal from being turned into dye... or bone blocks...

        if (event.recipe.result.type == Material.WHITE_DYE || event.recipe.result.type == Material.BONE_BLOCK) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun playerItemConsumeEvent(event: PlayerItemConsumeEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun entityDeathEvent(event: EntityDeathEvent) {
        event.droppedExp = 0

        when(event.entity.type) {
            EntityType.SPIDER -> {
                event.drops.clear()
                currentMap.world.dropItemNaturally(event.entity.location, ItemStack.of(Material.SPIDER_EYE, 1))
            }
            EntityType.COW -> {
                event.drops.clear()
                currentMap.world.dropItemNaturally(event.entity.location, ItemStack.of(Material.BEEF, 1))
            }
            EntityType.CHICKEN -> {
                event.drops.clear()
                currentMap.world.dropItemNaturally(event.entity.location, ItemStack.of(Material.CHICKEN, 1))
            }
            else -> {}
        }
    }

    @EventHandler
    fun playerPickupExperienceEvent(event: PlayerPickupExperienceEvent) {
        event.experienceOrb.remove()
        event.isCancelled = true
    }

    @EventHandler
    fun blockExpEvent(event: BlockExpEvent) {
        event.expToDrop = 0
    }

    @EventHandler
    fun inventoryClickEvent(event: InventoryClickEvent) {
        if (event.clickedInventory?.holder is Player && event.inventory.holder is Chest) {
            event.isCancelled = true
        }

        if (event.clickedInventory?.holder is Player && event.inventory.type == InventoryType.FURNACE &&
            (event.currentItem?.type == Material.BOWL || event.currentItem?.type == Material.WOODEN_SWORD) ||
            (event.cursor.type == Material.BOWL || event.cursor.type == Material.WOODEN_SWORD)
            ) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun playerRespawnEvent(event: PlayerRespawnEvent) {
        val player = event.player
        val tumblingPlayer = player.tumblingPlayer

        val playerSpawn = currentMap.data.getList("spawn")?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Spawn not found")

        val playerLocation = offsetLocation(playerSpawn, tumblingPlayer.team)

        event.respawnLocation = playerLocation

        kit.forEach { item ->
            player.inventory.addItem(item)
        }
    }

    enum class SnifferCaretakerScoreSource(override val id: String) : ScoreSource {
        TASK_1_STAR("sniffer_caretaker_task_1_star"),
        TASK_2_STAR("sniffer_caretaker_task_2_star"),
        TASK_3_STAR("sniffer_caretaker_task_3_star"),
        TASK_4_STAR("sniffer_caretaker_task_4_star"),
        TASK_5_STAR("sniffer_caretaker_task_5_star")
    }
}