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
import org.bukkit.attribute.Attribute
import org.bukkit.block.data.type.Gate
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
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
import xyz.devcmb.tumblers.util.forEachRegion
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
        Flag.DISABLE_PVP,
        Flag.DISABLE_BLOCK_BREAKING
    ),
    scores = hashMapOf(),
    icon = Component.empty(),
    scoreboard = "deathrunScoreboard"
) {
    companion object {
        @field:Configurable("games.deathrun.lives")
        var lives: Int = 3
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

    val attackingPlayers: Set<Player>
        get() {
            return gameParticipants.filter { it.tumblingPlayer.team == currentTeam }.toSet()
        }

    val runningPlayers: Set<Player>
        get() {
            return gameParticipants.filter { it.tumblingPlayer.team != currentTeam }.toSet()
        }

    val traps: ArrayList<Class<out Trap>> = ArrayList()
    val mapTraps: HashMap<Int, ArrayList<Trap>> = HashMap()
    val currentTraps: HashMap<Player, Int> = HashMap()
    val cooldowns: MutableSet<Int> = HashSet()

    val alivePlayers: MutableSet<Player> = HashSet()

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

        suspendSync {
            gamePlayers.forEach {
                if(it.tumblingPlayer.team == currentTeam) {
                    spawnAttacker(it)
                } else {
                    spawnMain(it)
                }
            }
        }
    }

    fun spawnMain(player: Player) {
        val mainSpawn: List<Double> = currentMap.data.getList("spawns.main")?.map {
            if(it !is Double) throw GameControllerException("Spawn locations do not contain exclusively doubles")
            it
        } ?: throw GameControllerException("Main spawn set not found")

        val mainLocation = mainSpawn.unpackCoordinates(currentMap.world)
        player.teleport(mainLocation)
    }

    fun spawnAttacker(player: Player) {
        val attackerSpawn: List<Double> = currentMap.data.getList("spawns.attacker")?.map {
            if(it !is Double) throw GameControllerException("Spawn locations do not contain exclusively doubles")
            it
        } ?: throw GameControllerException("Spawn set not found")

        val attackerLocation = attackerSpawn.unpackCoordinates(currentMap.world)
        player.teleport(attackerLocation)
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
            roundStart()
            countdown(60) // todo: replace
            roundActive = false
            postRound()
        }
    }

    suspend fun preRound() {
        alivePlayers.addAll(runningPlayers)
        alivePlayers.forEach {
            it.getAttribute(Attribute.MAX_HEALTH)?.baseValue = lives.toDouble() * 2
        }

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

    suspend fun postRound() {
        alivePlayers.clear()
        suspendSync {
            gameParticipants.forEach {
                it.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 20.0
                it.heal(20.0)
            }

            attackingPlayers.forEach {
                it.removePotionEffect(PotionEffectType.SPEED)
                it.inventory.clear()
            }
        }
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {
        delay(2000)
    }

    suspend fun roundStart() {
        val gateStart: Location = currentMap.data.getList("gate_start")?.map {
            if(it !is Int) throw GameControllerException("Location list does not contain exclusively doubles")
            it.toDouble()
        }?.unpackCoordinates(currentMap.world)
            ?: throw GameControllerException("Gate start not found")

        val gateEnd: Location = currentMap.data.getList("gate_end")?.map {
            if(it !is Int) throw GameControllerException("Location list does not contain exclusively doubles")
            it.toDouble()
        }?.unpackCoordinates(currentMap.world)
            ?: throw GameControllerException("Gate end not found")

        suspendSync {
            attackingPlayers.forEach {
                giveTrapItem(it, it.location)
                it.addPotionEffect(PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1))
            }

            gateStart.forEachRegion(gateEnd) {
                it.blockData = (it.blockData as Gate).also { gate ->
                    gate.isOpen = true
                }
            }
        }
    }

    fun giveTrapItem(player: Player, pos: Location) {
        val index = mapTraps[roundIndex]!!.indexOfFirst { pos.isInRegion(it.from, it.to) }
        if(index == -1) return

        val playerTrap = currentTraps[player]
        if(playerTrap == index) return

        currentTraps[player] = index
        player.inventory.clear()

        val trap = mapTraps[roundIndex]!![index]
        player.inventory.addItem(AdvancedItemStack(Material.PAPER) {
            name(trap.name)
            droppable(false)
            model(trap.itemKey)

            rightClick {
                if(player.tumblingPlayer.team != currentTeam) return@rightClick

                if(cooldowns.contains(index)) {
                    player.sendMessage(Format.error("This trap is currently on cooldown!"))
                    return@rightClick
                }

                cooldowns.add(index)
                TreeTumblers.pluginScope.launch {
                    val round = currentRound
                    trap.activate()
                    delay((trap.cooldown * 1000).toLong())
                    if(round == currentRound) cooldowns.remove(index)
                }
            }
        }.build())
    }

    @EventHandler
    fun attackerMoveEvent(event: PlayerMoveEvent) {
        val player = event.player
        if(player.tumblingPlayer.team != currentTeam || !roundActive) return

        val pos = event.to
        giveTrapItem(event.player, pos)
    }

    @EventHandler
    fun openGateEvent(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return

        if(block.blockData is Gate) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun playerDamageEvent(event: EntityDamageEvent) {
        val player = event.entity
        if(player !is Player) return

        event.damage = 2.0
        spawnMain(player)
    }

    @EventHandler
    fun playerHealEvent(event: EntityRegainHealthEvent) {
        if(event.entity !is Player || event.regainReason == EntityRegainHealthEvent.RegainReason.CUSTOM) return
        event.isCancelled = true
    }

    class DeathrunTrapException(override val message: String) : Exception()
}