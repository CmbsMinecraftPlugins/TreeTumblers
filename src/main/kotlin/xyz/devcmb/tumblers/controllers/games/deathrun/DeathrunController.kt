package xyz.devcmb.tumblers.controllers.games.deathrun

import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.persistence.PersistentDataType
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.games.deathrun.traps.MagmaFallTrap
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.enableBossBar
import xyz.devcmb.tumblers.util.isInRegion
import xyz.devcmb.tumblers.util.item.AdvancedItemStack
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.unpackCoordinates
import kotlin.math.max

@EventGame
class DeathrunController : GameBase(
    id = "deathrun",
    votable = true,
    maps = setOf(
        Map("forest")
    ),
    cutsceneSteps = arrayListOf(
        CutsceneStep(Component.empty()
            .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
            .append(Component.text("\uEA00").font(NamespacedKey("tumbling", "games/crumble")))
            .append(Component.text(" Deathrun"))
        ) {
            teleportConfig("cutscene.start")
            delay(5000)
        }
    ),
    flags = setOf(
        Flag.DISABLE_FALL_DAMAGE,
        Flag.DISABLE_PVP
    ),
    scores = hashMapOf(),
    icon = Component.empty(),
    scoreboard = "deathrunScoreboard"
) {
    companion object {
        val trapKey = NamespacedKey("tumbling", "deathrun_trap")
    }

    val playingTeams = Team.entries.filter { it.playingTeam }
    val rounds = playingTeams.size
    var currentRound = 0
    val roundIndex
        get() = max(currentRound - 1, 0)
    var roundActive = false

    val currentMap: LoadedMap
        get() {
            return loadedMaps[roundIndex]
        }

    val currentTeam: Team
        get() {
            return playingTeams[roundIndex]
        }

    val traps: ArrayList<Class<out Trap>> = ArrayList()
    val mapTraps: HashMap<Int, ArrayList<Trap>> = HashMap()
    val currentTraps: HashMap<Player, Int> = HashMap()
    val cooldowns: MutableSet<Int> = HashSet()

    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun gameLoad() {
        traps.add(MagmaFallTrap::class.java)

        repeat(rounds) {
            val map = loadMap(maps.random(), it + 1)

            val traps: ArrayList<Trap> = ArrayList(map.data.getList("traps")?.map { trap ->
                if(
                    trap !is HashMap<*,*>
                    || trap["id"] == null
                    || trap["id"] !is String
                    || trap["start"] !is List<*>
                    || trap["end"] !is List<*>
                    || (trap["start"] as List<*>).find { element -> element !is Int } != null
                    || (trap["end"] as List<*>).find { element -> element !is Int } != null
                    || trap["data"] == null
                    || trap["data"] !is HashMap<*,*>
                ) throw GameControllerException("Trap definition is not formatted correctly")

                val id = trap["id"]
                val trapClass = traps.find { clazz ->
                    val ins = clazz.getDeclaredConstructor(
                        DeathrunController::class.java,
                        ConfigurationSection::class.java,
                        Location::class.java,
                        Location::class.java
                    ).newInstance(
                        this,
                        YamlConfiguration().createSection("blah.blah.blah"),
                        Location(currentMap.world,0.0,0.0,0.0),
                        Location(currentMap.world,0.0,0.0,0.0)
                    )
                    
                    ins.id == id
                }?.getDeclaredConstructor(
                    DeathrunController::class.java,
                    ConfigurationSection::class.java,
                    Location::class.java,
                    Location::class.java
                ) ?: throw GameControllerException("Trap $id not found")

                // I tried doing `map.data.getConfigurationSection("traps.$index.data")` but that just always returned null no matter what
                // So this copilot fix is the best I've got :sob:
                // this took too long
                val data = YamlConfiguration().createSection("data", trap["data"] as HashMap<*,*>)

                val start = (trap["start"] as List<Int>).map { v -> v.toDouble() }.unpackCoordinates(currentMap.world)
                val end = (trap["end"] as List<Int>).map { v -> v.toDouble() }.unpackCoordinates(currentMap.world)
                trapClass.newInstance(this, data, start, end)
            } ?: throw GameControllerException("Traps list not found"))

            mapTraps.put(it, traps)
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
        if(cycle != SpawnCycle.PRE_ROUND) return

        val mainSpawn: List<Double> = currentMap.data.getList("spawns.main")?.map {
            if(it !is Double) throw GameControllerException("Spawn locations do not contain exclusively doubles")
            it
        } ?: throw GameControllerException("Main spawn set not found")

        val attackerSpawn: List<Double> = currentMap.data.getList("spawns.attacker")?.map {
            if(it !is Double) throw GameControllerException("Spawn locations do not contain exclusively doubles")
            it
        } ?: throw GameControllerException("Spawn set not found")

        val mainLocation = mainSpawn.unpackCoordinates(currentMap.world)
        val attackerLocation = attackerSpawn.unpackCoordinates(currentMap.world)

        suspendSync {
            gamePlayers.forEach {
                val location = if(it.tumblingPlayer.team == currentTeam) attackerLocation else mainLocation
                it.teleport(location)
            }
        }
    }

    /**
     * The method for the main gameplay loop for an individual game
     *
     * This should contain any kind of game-specific logic, and round handling if applicable
     */
    override suspend fun gameOn() {
        Bukkit.getOnlinePlayers().forEach {
            it.enableBossBar("countdownBossbar")
        }

        repeat(rounds) {
            currentRound++
            cooldowns.clear()
            currentTraps.clear()
            spawn(SpawnCycle.PRE_ROUND)
            asyncCountdown(10) {}
            preRound()
            roundActive = true
            countdown(300) // todo: replace
            roundActive = false
        }
    }

    suspend fun preRound() {
        delay(1000)
        val audience = Audience.audience(gamePlayers)
        val title = Title.title(
            Format.mm("<bold><yellow>Round $currentRound</yellow></bold>"),
            Format.mm("<team> are up!", Placeholder.component("team", currentTeam.formattedName)),
            Title.Times.times(Tick.of(5), Tick.of(80), Tick.of(5))
        )

        audience.showTitle(title)

        delay(4000)
        MiscUtils.subtitleCountdown(
            audience,
            Format.mm("<bold><yellow>Round $currentRound</yellow></bold>"),
            5
        )
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {
        delay(2000)
    }

    fun setCurrentTrap(player: Player, index: Int) {
        currentTraps[player] = index
        player.inventory.clear()

        val trap = mapTraps[roundIndex]!![index]
        player.inventory.addItem(AdvancedItemStack(Material.PAPER) {
            name(trap.name)
            droppable(false)
            persistentDataContainer {
                set(trapKey, PersistentDataType.INTEGER, index)
            }
            model(trap.itemKey)

            rightClick {
                if(cooldowns.contains(index)) {
                    player.sendMessage(Format.error("This trap is currently on cooldown!"))
                    return@rightClick
                }

                cooldowns.add(index)
                TreeTumblers.pluginScope.launch {
                    trap.activate()
                }
            }
        }.build())
    }

    @EventHandler
    fun attackerMoveEvent(event: PlayerMoveEvent) {
        val player = event.player
        if(player.tumblingPlayer.team != currentTeam || !roundActive) return

        val pos = event.to

        val currentTrap = mapTraps[roundIndex]!!.indexOfFirst { pos.isInRegion(it.from, it.to) }
        if(currentTrap == -1) return

        val playerTrap = currentTraps[player]

        if(playerTrap != currentTrap) {
            setCurrentTrap(player, currentTrap)
        }
    }

    class DeathrunTrapException(override val message: String) : Exception()
}