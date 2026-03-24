package xyz.devcmb.tumblers.controllers.games.sniffercaretaker

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Chest
import org.bukkit.block.data.Ageable
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
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
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.tasks.HungryTask
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
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.unpackCoordinates
import xyz.devcmb.tumblers.util.validateCoordinates

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
        var gameLength: Int = 120

        @field:Configurable("games.snifferCaretaker.chest_refresh")
        var chestRefresh: Long = 10
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
        Material.DIRT
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

    val currentTasks: HashMap<Team, MutableList<Task>> = hashMapOf()

    fun stockChests(team: Team) {
        // TODO: there will be MORE chests later. un hard code it later!
        val chestPosition = currentMap.data.getList("supplyChests.farm.${team.name.lowercase()}")?.validateCoordinates()
            ?: throw GameControllerException("Chest position not found")

        val chestLocation = chestPosition.unpackCoordinates(currentMap.world)
        currentMap.world.getBlockAt(chestLocation).type = Material.CHEST

        val chest = chestLocation.block.state as Chest
        val inventory = chest.inventory

        inventory.setItem(0, ItemStack(Material.WHEAT_SEEDS, 10))
    }

    fun completeTask(team: Team, task: Task) {
        DebugUtil.info("completed a task!")
        currentTasks.get(team)?.remove(task)
    }

    fun createNewTask(team: Team) {
        val tasks = TreeTumblers.plugin.config.getList("games.snifferCaretaker.tasks")
        val chosenTask = tasks?.random() as HashMap<*, *>

        val stars = chosenTask["stars"]
        if (stars !is Int) {return}

        val item = chosenTask["item"]
        if (item !is String) {return}

        val createdTask = when (chosenTask["type"]) {
            "HUNGRY" -> HungryTask(
                team,
                this,
                stars,
                Material.getMaterial(item)
            )
            else -> throw GameControllerException("Task type invalid")
        }

        currentTasks.get(team)!!.add(createdTask)
        Bukkit.getServer().pluginManager.registerEvents(createdTask, TreeTumblers.plugin)

        team.getOnlinePlayers().forEach { player ->
            player.sendMessage("Your sniffer is ${createdTask.feeling}! ${createdTask.description}")
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

        suspendSync {
            Team.entries.filter { it.playingTeam }.forEach {
                currentTasks[it] = mutableListOf()

                val snifferSpawn = map.data.getList("sniffer_spawns.${it.name.lowercase()}")?.validateCoordinates()
                    ?: throw GameControllerException("Sniffer spawns not found")

                val snifferLocation = snifferSpawn.unpackCoordinates(map.world)
                val sniffer = map.world.spawnEntity(snifferLocation, EntityType.SNIFFER)

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

                val playerSpawn = currentMap.data.getList("spawns.${tumblingPlayer.team.name.lowercase()}")?.validateCoordinates()
                    ?: throw GameControllerException("Spawn not found")

                val playerLocation = playerSpawn.unpackCoordinates(map.world)

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
        val task = object : BukkitRunnable() {
            override fun run() {
                Team.entries.filter { it.playingTeam }.forEach {
                    stockChests(it)
                }
            }
        }

        task.runTaskTimer(TreeTumblers.plugin, 20*chestRefresh, 20*chestRefresh)

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