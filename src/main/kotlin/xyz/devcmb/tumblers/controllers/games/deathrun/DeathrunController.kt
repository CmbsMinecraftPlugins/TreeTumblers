package xyz.devcmb.tumblers.controllers.games.deathrun

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.block.data.type.Gate
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.games.deathrun.traps.*
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.DebugToolkit
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.engine.score.ScoreSource
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.disableBossBar
import xyz.devcmb.tumblers.util.enableBossBar
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.hideToAll
import xyz.devcmb.tumblers.util.isInRegion
import xyz.devcmb.tumblers.util.item.AdvancedItemStack
import xyz.devcmb.tumblers.util.showToAll
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
            .append(Component.text("\uEA00").font(NamespacedKey("tumbling", "games/deathrun")))
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
    scores = hashMapOf(
        DeathrunScoreSource.RUN_COMPLETE to 140,
        DeathrunScoreSource.RUN_FAILED to 20,
        DeathrunScoreSource.TRAP_KILL to 10,
        DeathrunScoreSource.TRAP_DAMAGE to 5
    ),
    icon = Component.text("\uEA00").font(font),
    scoreboard = "deathrunScoreboard"
) {
    companion object {
        val font = NamespacedKey("tumbling", "games/deathrun")

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

    override val scoreMessages: HashMap<ScoreSource, (Int) -> Component> = hashMapOf(
        DeathrunScoreSource.RUN_COMPLETE to { amount ->
            gameMessage(
                Component.text("Run complete! ")
                    .append(Component.text("[+$amount]", NamedTextColor.GOLD))
            )
        },
        DeathrunScoreSource.RUN_FAILED to { amount ->
            gameMessage(
                Component.text("Run failed! ")
                    .append(Component.text("[+$amount]", NamedTextColor.GOLD))
            )
        }
    )

    override val debugToolkit: DebugToolkit? = object : DebugToolkit() {
        override val events: HashMap<String, (CommandSender) -> Unit> = hashMapOf()

        override fun killEvent(killer: Player?, killed: Player?) {
            killed?.damage(1.0)
        }

        override fun deathEvent(killed: Player?) {
            killed?.damage(1.0)
        }
    }

    val traps: ArrayList<Class<out Trap>> = ArrayList()
    val mapTraps: HashMap<Int, ArrayList<Trap>> = HashMap()
    val currentTraps: HashMap<Player, Int> = HashMap()
    val cooldowns: MutableSet<Int> = HashSet()

    val alivePlayers: MutableSet<Player> = HashSet()
    val spectators: MutableSet<Player> = HashSet()

    var ticksElapsed: Int = 0
    val completionTimes: HashMap<Player, Int> = HashMap()
    var timerActionBarTasks: HashMap<Player, BukkitRunnable> = HashMap()

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
        traps.add(BeamRunTrap::class.java)
        traps.add(HappyGhastTrap::class.java)

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
        gameParticipants.forEach {
            val runnable = object : BukkitRunnable() {
                override fun run() {
                    val time = completionTimes.getOrElse(it) { ticksElapsed }
                    val text = MiscUtils.formatMsTime(time * 50L)

                    it.sendActionBar(UserInterfaceUtility.backgroundTextCenter(
                        Component.text("\uEF00").font(font).shadowColor(ShadowColor.shadowColor(0)),
                        Component.text(text),
                        text,
                        69.5
                    ))
                }
            }
            runnable.runTaskTimer(TreeTumblers.plugin, 0, 1)
            timerActionBarTasks.put(it, runnable)
        }
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

    var roundEnded = false
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
            asyncCountdown(120) {
                roundEnded = true
            }

            while(!roundEnded) {
                delay(500)
            }

            roundActive = false
            roundEnd()
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
        audience.sendMessage(gameMessage(
            Format.mm("Round $currentRound: <team> are up!", Placeholder.component("team", currentTeam.formattedName))
        ))

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
        if(currentRound != rounds) {
            spectators.forEach {
                it.showToAll()
                it.isFlying = false
                it.allowFlight = false
            }
        }

        alivePlayers.clear()
        spectators.clear()
        completionTimes.clear()
        ticksElapsed = 0

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
        timerActionBarTasks.forEach {
            it.value.cancel()
            it.key.sendActionBar(Component.empty())
        }
        timerActionBarTasks.clear()

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
        Bukkit.getOnlinePlayers().forEach {
            it.disableBossBar("countdownBossbar")
        }
        super.cleanup()
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

    fun makeSpectator(player: Player) {
        alivePlayers.remove(player)
        spectators.add(player)
        player.hideToAll()
        player.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 20.0
        player.heal(20.0)
        player.allowFlight = true
        player.isFlying = true

        if(alivePlayers.isEmpty()) {
            roundEnded = true
        }
    }

    fun completeRun(player: Player) {
        makeSpectator(player)
        Audience.audience(Bukkit.getOnlinePlayers()).sendMessage(
            gameMessage(Format.mm(
                "<green><player> has completed the run in <white>${MiscUtils.formatMsTime(ticksElapsed * 50L)}</white>!</green>",
                Placeholder.component("player", Format.formatPlayerName(player))
            ))
        )
        completionTimes.put(player, ticksElapsed)
        MiscUtils.spawnFirework(player, FireworkEffect.builder()
            .trail(false)
            .flicker(false)
            .withColor(Color.YELLOW)
            .withColor(Color.ORANGE)
            .with(FireworkEffect.Type.BALL_LARGE)
            .build()
        )

        grantScore(player, DeathrunScoreSource.RUN_COMPLETE)
        player.showTitle(Title.title(
            Component.empty(),
            Format.success("Run complete")
                .append(Component.text(" [+${getScoreSource(DeathrunScoreSource.RUN_COMPLETE)}]", NamedTextColor.GOLD)),
            Title.Times.times(Tick.of(5), Tick.of(40), Tick.of(5))
        ))
    }

    fun failRun(player: Player) {
        makeSpectator(player)
        grantScore(player, DeathrunScoreSource.RUN_FAILED)
        Audience.audience(Bukkit.getOnlinePlayers()).sendMessage(
            gameMessage(Format.mm(
                "<red><player> has been eliminated!</red>",
                Placeholder.component("player", Format.formatPlayerName(player))
            ))
        )
        currentTeam.getOnlinePlayers().forEach {
            grantScore(it, DeathrunScoreSource.TRAP_KILL)
        }

        player.showTitle(Title.title(
            Component.empty(),
            Format.error("Run failed")
                .append(Component.text(" [+${getScoreSource(DeathrunScoreSource.RUN_FAILED)}]", NamedTextColor.GOLD)),
            Title.Times.times(Tick.of(5), Tick.of(40), Tick.of(5))
        ))
    }

    suspend fun roundEnd() {
        roundEnded = false
        cancelCountdown()
        Bukkit.getOnlinePlayers().forEach {
            it.sendMessage(gameMessage(Component.text("Round Over!")))
            it.showTitle(Title.title(
                Format.mm("<red><bold>Round Over</bold></red>"),
                Component.empty(),
                Title.Times.times(Tick.of(0), Tick.of(50), Tick.of(10))
            ))
        }
        delay(4000)
    }

    @EventHandler
    fun attackerMoveEvent(event: PlayerMoveEvent) {
        val player = event.player
        if(player.tumblingPlayer.team != currentTeam || !roundActive) return

        val pos = event.to
        giveTrapItem(event.player, pos)
    }

    @EventHandler
    fun runnerMoveEvent(event: PlayerMoveEvent) {
        val player = event.player
        if(
            !roundActive
            || !alivePlayers.contains(player)
        ) return

        val pos = event.to
        val winStart = currentMap.data.getList("win_zone_start")?.map {
            if(it !is Int) throw GameControllerException("Location list does not contain exclusively doubles")
            it.toDouble()
        }?.unpackCoordinates(currentMap.world)
            ?: throw GameControllerException("Win start location not found")

        val winEnd = currentMap.data.getList("win_zone_end")?.map {
            if(it !is Int) throw GameControllerException("Location list does not contain exclusively doubles")
            it.toDouble()
        }?.unpackCoordinates(currentMap.world)
            ?: throw GameControllerException("Win end location not found")

        if(pos.isInRegion(winStart, winEnd)) {
            completeRun(player)
        }
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
        if(event.isCancelled) return

        val player = event.entity
        if(player !is Player) return

        if(!alivePlayers.contains(player)) {
            event.isCancelled = true
            return
        }

        event.damage = 2.0
        Bukkit.getOnlinePlayers().forEach {
            it.sendMessage(
                Format.formatDeathMessage(
                    player,
                    it,
                    it.tumblingPlayer.team == currentTeam,
                    getScoreSource(DeathrunScoreSource.TRAP_DAMAGE)
                )
            )

            if(it.tumblingPlayer.team == currentTeam) {
                grantScore(it, DeathrunScoreSource.TRAP_DAMAGE)
            }
        }
        if(player.health - 2.0 > 0) spawnMain(player)
    }

    @EventHandler
    fun playerRunFail(event: PlayerDeathEvent) {
        event.isCancelled = true
        failRun(event.player)
    }

    @EventHandler
    fun playerHealEvent(event: EntityRegainHealthEvent) {
        if(event.entity !is Player || event.regainReason == EntityRegainHealthEvent.RegainReason.CUSTOM) return
        event.isCancelled = true
    }

    @EventHandler
    fun tickEvent(event: ServerTickStartEvent) {
        if(!roundActive) return
        ticksElapsed++
    }

    @EventHandler
    fun moveEvent(event: PlayerMoveEvent) {
        if(!roundActive) return

        val voidHeight = currentMap.data.getInt("void_height")
        if(event.to.y < voidHeight) {
            event.player.damage(1.0)
        }
    }

    class DeathrunTrapException(override val message: String) : Exception()

    enum class DeathrunScoreSource(override val id: String) : ScoreSource {
        RUN_COMPLETE("deathrun_run_complete"),
        RUN_FAILED("deathrun_run_failed"),
        TRAP_DAMAGE("deathrun_trap_damage"),
        TRAP_KILL("deathrun_trap_kill")
    }
}