package xyz.devcmb.tumblers.controllers.games.tower_ascent

import net.kyori.adventure.audience.Audience
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.games.tower_ascent.data.TowerAscentData
import xyz.devcmb.tumblers.controllers.games.tower_ascent.data.TowerAscentSpawn
import xyz.devcmb.tumblers.controllers.games.tower_ascent.feature.TowerGenerator
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.Timer
import xyz.devcmb.tumblers.engine.base.AbstractGame
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.item.Kit
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.giveKit
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.titleCountdown
import java.util.HashMap
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@EventGame
class TowerAscentController : AbstractGame(TowerAscentData) {
    val map: LoadedMap
        get() = loadedMaps.first()

    lateinit var generator: TowerGenerator

    val teamRoomSetIndexes: HashMap<Team, Int> = HashMap()
    val teamRooms: HashMap<Team, Int>
        get() = HashMap(generator.towerHandlers.associate { it.team to it.currentRoomIndex })
    val teamCompletedRooms: HashMap<Team, Int> = HashMap()

    val playerKit: Kit.KitDefinition = object : Kit.KitDefinition {
        override val items: ArrayList<Kit.KitItem> = arrayListOf(
            Kit.KitItem.StandardItem(ItemStack(Material.IRON_SWORD)),
            Kit.KitItem.StandardItem(ItemStack(Material.IRON_PICKAXE)),
            Kit.KitItem.StandardItem(ItemStack(Material.COOKED_BEEF, 4)),

            Kit.KitItem.ArmorItem(ItemStack.of(Material.LEATHER_HELMET)),
            Kit.KitItem.ArmorItem(ItemStack.of(Material.IRON_CHESTPLATE)),
            Kit.KitItem.ArmorItem(ItemStack.of(Material.LEATHER_LEGGINGS)),
            Kit.KitItem.ArmorItem(ItemStack.of(Material.LEATHER_BOOTS)),
        )
        override val defaultDropability: Boolean = true
        override val uuid: UUID = UUID.randomUUID()
    }

    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    override suspend fun gameLoad() {
        val map = loadMap(data.maps.random(), 1)

        generator = TowerGenerator(this, map)
        generator.generateTowers()
    }

    /**
     * The abstract method for spawning players in
     *
     * There was going to be some kind of system to do this automatically, but doing it manually seems to be a more flexible option, at least for now.
     *
     * @param cycle The stage where the players are spawned
     */
    override suspend fun spawn(cycle: SpawnCycle) {
        if(cycle != SpawnCycle.PREGAME) return

        suspendSync {
            Team.nonPlayingTeams.forEach {
                spawnPlayers(map, it.getOnlinePlayers(), TowerAscentSpawn.SET_1)
            }

            Team.playingTeams.forEachIndexed { index, team ->
                teamRoomSetIndexes[team] = index
                teamCompletedRooms[team] = 0

                generator.towerHandlers.getOrNull(index)?.team = team
                generator.mapSpawns.getOrNull(index)?.let {
                    it.wallBounds.first.forEachRegion(it.wallBounds.second) { block ->
                        block.type = team.glass
                    }
                }

                spawnPlayers(map, team.getOnlinePlayers(), TowerAscentSpawn.valueOf("SET_${index + 1}"))
                team.getOnlinePlayers().forEach {
                    it.giveKit(playerKit)
                }
            }
        }
    }

    override suspend fun gamePregame() {
        timer(Timer(20.seconds) {
            id = "tower_ascent_game_start"
            title = "Game Start"
            joined = true

            timeExecution(10) {
                titleCountdown(
                    Audience.audience(gamePlayers.mapNotNull { it.bukkitPlayer }),
                    Format.mm("Game starts in"),
                    10
                )
            }
        })

        suspendSync {
            generator.mapSpawns.forEach {
                it.wallBounds.first.forEachRegion(it.wallBounds.second) { block ->
                    block.type = Material.AIR
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
        generator.towerHandlers.forEach {
            it.startGame()
        }

        timer(Timer(12.minutes) {
            id = "tower_ascent_game_on"
            title = "Game Over"
            joined = true
        })
    }

    override suspend fun cleanup() {
        generator.cleanup()
        super.cleanup()
    }

    /**
     * The method that gets called when a player joins the game during the [State.GAME_ON] and [State.PREGAME] states
     */
    override fun playerJoin(player: Player) {
        TODO("Not yet implemented")
    }

    /**
     * The method that gets called when a player leaves the game during the [State.GAME_ON] and [State.PREGAME] state
     */
    override fun playerLeave(player: Player) {
        TODO("Not yet implemented")
    }
}