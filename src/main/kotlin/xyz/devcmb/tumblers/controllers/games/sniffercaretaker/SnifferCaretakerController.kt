package xyz.devcmb.tumblers.controllers.games.sniffercaretaker

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.regions.CuboidRegion
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Chest
import org.bukkit.block.data.Ageable
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.tasks.BoredTask
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.tasks.HungryTask
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.tasks.ThirstyTask
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.DebugToolkit
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.enableBossBar
import xyz.devcmb.tumblers.util.fill
import xyz.devcmb.tumblers.util.randomBetween
import xyz.devcmb.tumblers.util.toBlockVector3
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.unpackCoordinates
import xyz.devcmb.tumblers.util.validateCoordinates
import java.util.UUID
import kotlin.collections.forEach

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
                .append(Component.text("\uEA00").font(NamespacedKey("tumbling", "games/crumble")))
                .append(Component.text(" Sniffer Caretaker"))
        ) { map ->
            teleportConfig("cutscene.start")
            delay(5000)
        },
    ),
    flags = emptySet(),
    scores = hashMapOf(),
    icon = Component.empty(),
    scoreboard = "snifferCaretakerScoreboard"
) {
    companion object {
        val snifferTeamKey = NamespacedKey("tumbling", "sniffer_team")

        @field:Configurable("games.snifferCaretaker.game_length")
        var gameLength: Int = 600

        @field:Configurable("games.snifferCaretaker.chest_refresh")
        var chestRefresh: Long = 20

        @field:Configurable("games.snifferCaretaker.block_refresh")
        var blockRefresh: Long = 15
    }

    override val debugToolkit = object : DebugToolkit() {
        override val events: HashMap<String, (sender: CommandSender) -> Unit> = hashMapOf(
            "createtask" to { sender ->
                if (sender !is Player) {
                    sender.sendMessage(Format.error("Only players can trigger this event!"))
                    return@to
                }

                Team.entries.filter { it.playingTeam }.forEach {
                    createNewTask(it)
                }

                sender.sendMessage(Format.success("Created a new task!"))
            }
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
        Material.MUD
    )

    val kit: List<ItemStack> = listOf(
        ItemStack(Material.STONE_PICKAXE).apply {
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
    )

    val chestItems: List<List<*>> = listOf(
        listOf(Material.WHEAT_SEEDS, 8),
        listOf(Material.PUMPKIN_SEEDS, 1),
        listOf(Material.SUGAR_CANE, 1)
    )

    val blockItems: HashMap<String, List<List<*>>> = hashMapOf(
        "dirt" to listOf(
            listOf(Material.DIRT, 9)
        ),
        "moss" to listOf(
            listOf(Material.STONE, 6),
            listOf(Material.MOSS_BLOCK, 2)
        )
    )

    var timers = mutableMapOf(
        "supply_chest" to chestRefresh * 20,
        "dirt" to blockRefresh * 20,
        "moss" to blockRefresh * 20
    )

    val timerBases = hashMapOf(
        "supply_chest" to chestRefresh * 20,
        "dirt" to blockRefresh * 20,
        "moss" to blockRefresh * 20
    )

    val currentTasks: HashMap<Team, MutableList<Task>> = hashMapOf()

    val signs: HashMap<Team, HashMap<String, TextDisplay>> = hashMapOf()

    fun offsetLocation(location: Location, team: Team): Location {
        return location.add((team.priority - 1) * 1000.0, 0.0, 0.0)
    }

    fun stockChests(team: Team) {
        val chestPosition = currentMap.data.getList("supply_chest")?.validateCoordinates()
            ?: throw GameControllerException("Chest position not found")

        val chestLocation = offsetLocation(chestPosition.unpackCoordinates(currentMap.world), team)
        currentMap.world.getBlockAt(chestLocation).type = Material.CHEST

        val chest = chestLocation.block.state as Chest
        val inventory = chest.inventory

        inventory.clear()

        chestItems.forEach {
            val material = it[0] as Material
            val amount = it[1] as Int

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

    fun stockBlocks(team: Team) {
        blockItems.forEach { (key, it) ->
            val area = currentMap.data.getList("block_containments.$key.area")!!.map { it as List<*>
                it.validateCoordinates()
            }

//            val door = currentMap.data.getList("block_containments.$key.door")!!.map { it as List<*>
//                it.validateCoordinates()
//            }

            val areaMin = offsetLocation(area[0]!!.unpackCoordinates(currentMap.world), team)
            val areaMax = offsetLocation(area[1]!!.unpackCoordinates(currentMap.world), team)

//            val doorMin = door[0]!!.unpackCoordinates(currentMap.world)
//            val doorMax = door[1]!!.unpackCoordinates(currentMap.world)

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

    fun completeTask(team: Team, task: Task) {
        task.display?.remove()
        HandlerList.unregisterAll(task)

        val taskIndex = currentTasks[team]!!.indexOfFirst {
            it.id == task.id
        }

        DebugUtil.info("$taskIndex")

        if (taskIndex == -1) return

        currentTasks[team]!!.removeAt(taskIndex)

        val displaySpawn = currentMap.data.getList("task_board")?.validateCoordinates()
            ?: throw GameControllerException("Task board spawn not found")

        currentTasks[team]!!.forEachIndexed { index, it ->
            val displayLocation = offsetLocation(displaySpawn.unpackCoordinates(currentMap.world), team)
                .add(0.0, index * 0.5, 0.0)

            it.display?.teleport(displayLocation)
        }

    }

    fun createNewTask(team: Team) {
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

        val filteredTasks = tasks.filter {
            currentTasks[team]!!.find { task ->
                task.id == it["id"]
            } == null
        }

        if (filteredTasks.isEmpty()) return

        val chosenTask = filteredTasks.random()

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
            else -> throw GameControllerException("Task type invalid")
        }

        val displaySpawn = currentMap.data.getList("task_board")?.validateCoordinates()
            ?: throw GameControllerException("Task board spawn not found")

        val displayLocation = offsetLocation(displaySpawn.unpackCoordinates(currentMap.world), team)
            .add(0.0, currentTasks[team]!!.size * 0.5, 0.0)

        val display: TextDisplay = currentMap.world.spawnEntity(displayLocation, EntityType.TEXT_DISPLAY) as TextDisplay
        display.alignment = TextDisplay.TextAlignment.LEFT
        display.lineWidth = 400

        display.text(createdTask.displayText)

        createdTask.display = display

        currentTasks.get(team)!!.add(createdTask)
        Bukkit.getServer().pluginManager.registerEvents(createdTask, TreeTumblers.plugin)

        team.getOnlinePlayers().forEach { player ->
            player.sendMessage(createdTask.displayText)
        }
    }

    fun setupSign(key: String, team: Team) : TextDisplay {
        val displaySpawn = currentMap.data.getList(key)?.validateCoordinates()
            ?: throw GameControllerException("Sign spawn not found at $key")

        val displayLocation = offsetLocation(displaySpawn.unpackCoordinates(currentMap.world), team)

        val display: TextDisplay = currentMap.world.spawnEntity(displayLocation, EntityType.TEXT_DISPLAY) as TextDisplay
        display.alignment = TextDisplay.TextAlignment.CENTER
        display.lineWidth = 400
        display.isPersistent = true
        display.text(Format.mm("WHAT AM I DOING"))

        return display
    }

    fun setupSigns(team: Team) {
        signs[team]!!["supply_chest"] = setupSign("supply_chest_sign", team)
        signs[team]!!["dirt"] = setupSign("block_containments.dirt.sign", team)
        signs[team]!!["moss"] = setupSign("block_containments.moss.sign", team)
    }

    fun updateSigns(team: Team) {
        signs[team]!!.forEach { key, it ->

            it.interpolationDelay = 0
            it.interpolationDuration = 0
            it.text(Component.text("Restocking in ${timers[key]}"))

            DebugUtil.info("${it.text()}")
            it.teleport(it.location.add(0.0, 1.0, 0.0))
        }
    }

    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    override suspend fun gameLoad() {
        val map = loadMap(maps.random(), 1)
        val adaptedWorld = BukkitAdapter.adapt(map.world)
        val session = WorldEdit.getInstance().newEditSession(adaptedWorld)

        val mapBounds = map.data.getList("base_map")!!.map { it as List<*>
            it.validateCoordinates()
        }

        val mapBound1 = mapBounds[0]!!.unpackCoordinates(map.world)
        val mapBound2 = mapBounds[1]!!.unpackCoordinates(map.world)

        val region = CuboidRegion(mapBound1.toBlockVector3(), mapBound2.toBlockVector3())

        repeat(7) {
            val forwardExtentCopy = ForwardExtentCopy(adaptedWorld, region, adaptedWorld, region.minimumPoint.add((it + 1) * 1000, 0, 0))
            Operations.complete(forwardExtentCopy)
        }

        session.close()

        suspendSync {
            Team.entries.filter { it.playingTeam }.forEach {
                currentTasks[it] = mutableListOf()

                signs[it] = hashMapOf()



                val snifferSpawn = map.data.getList("sniffer_spawn")?.validateCoordinates()
                    ?: throw GameControllerException("Sniffer spawns not found")

                val snifferLocation = offsetLocation(snifferSpawn.unpackCoordinates(map.world), it)
                val sniffer = map.world.spawnEntity(snifferLocation, EntityType.SNIFFER)

                sniffer.isInvulnerable = true

                sniffer.persistentDataContainer.set(
                    snifferTeamKey,
                    PersistentDataType.STRING,
                    it.name
                )

                // Five Big Booms

                stockChests(it)



            }
        }
    }

    override suspend fun gamePregame() {
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
                val tumblingPlayer = it.tumblingPlayer

                val playerSpawn = currentMap.data.getList("spawn")?.validateCoordinates()
                    ?: throw GameControllerException("Spawn not found")

                val playerLocation = offsetLocation(playerSpawn.unpackCoordinates(map.world), tumblingPlayer.team)

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
        spawn(SpawnCycle.PRE_ROUND)
        gamePlayers.forEach {
            it.enableBossBar("countdownBossbar")
        }


        suspendSync {
            Team.entries.filter { it.playingTeam }.forEach {
                setupSigns(it)
            }

            val task = object : BukkitRunnable() {
                override fun run() {
                    Team.entries.filter { it.playingTeam }.forEach {
                        stockChests(it)
                        stockBlocks(it)
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
                    }
                }
            }

            task.runTaskTimer(TreeTumblers.plugin, 20*chestRefresh, 20*chestRefresh)
            tickTask.runTaskTimer(TreeTumblers.plugin, 100, 20)
        }



        countdown(gameLength)
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {
        delay(1000)
    }

    @EventHandler
    fun blockBreakEvent(event: BlockBreakEvent) {
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

        // prevent farmland trampling!
        if (event.action == Action.PHYSICAL && event.clickedBlock?.type == Material.FARMLAND) {
            event.isCancelled = true
        }
    }
}