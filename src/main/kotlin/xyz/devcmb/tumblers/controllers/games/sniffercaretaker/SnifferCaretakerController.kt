package xyz.devcmb.tumblers.controllers.games.sniffercaretaker

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Chest
import org.bukkit.block.data.Ageable
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
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
    val currentMap: LoadedMap
        get() {
            return loadedMaps[0]
        }

    val breakableBlocks: List<Material> = listOf(
        Material.WHEAT_SEEDS,
        Material.WHEAT,
        Material.DIRT
    )

    fun stockChests(team: Team) {
        // TODO: there will be MORE chests later. un hard code it later!
        val chestPosition = currentMap.data.getList("supplyChests.farm.${team.name.lowercase()}")?.validateCoordinates()
            ?: throw GameControllerException("Chest position not found")

        val chestLocation = chestPosition.unpackCoordinates(currentMap.world)
        currentMap.world.getBlockAt(chestLocation).type = Material.CHEST

        val chest = currentMap.world.getBlockAt(chestLocation).state as Chest
        val inventory = chest.inventory

        inventory.setItem(0, ItemStack(Material.WHEAT_SEEDS, 10))
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
                val snifferSpawn = map.data.getList("sniffer_spawns.${it.name.lowercase()}")?.validateCoordinates()
                    ?: throw GameControllerException("Sniffer spawns not found")

                val snifferLocation = snifferSpawn.unpackCoordinates(map.world)
                map.world.spawnEntity(snifferLocation, EntityType.SNIFFER)

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

                it.inventory.addItem(ItemStack(Material.STONE_PICKAXE).apply {
                    itemMeta = itemMeta.also { item ->
                        item.isUnbreakable = true
                    }
                })

                it.inventory.addItem(ItemStack(Material.STONE_SHOVEL).apply {
                    itemMeta = itemMeta.also { item ->
                        item.isUnbreakable = true
                    }
                })

                it.inventory.addItem(ItemStack(Material.BONE_MEAL, 64))
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

        task.runTaskTimer(TreeTumblers.plugin, 20*10, 20*10)

        countdown(120)
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
        if (event.action == Action.RIGHT_CLICK_BLOCK && event.item?.type == Material.BONE_MEAL) {
            event.isCancelled = true
            event.clickedBlock?.applyBoneMeal(event.blockFace)
        }

        // prevent farmland trampling!
        if (event.action == Action.PHYSICAL && event.clickedBlock?.type == Material.FARMLAND) {
            event.isCancelled = true
        }
    }
}