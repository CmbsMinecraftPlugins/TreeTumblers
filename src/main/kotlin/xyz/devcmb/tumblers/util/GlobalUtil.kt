package xyz.devcmb.tumblers.util

import com.sk89q.worldedit.math.BlockVector3
import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.entity.Arrow
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.entity.Trident
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.generator.BiomeProvider
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Score
import xyz.devcmb.tumblers.ControllerRegistry
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.player.PlayerController
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.ui.PlayerUIController
import java.time.Duration
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val playerController: PlayerController by ControllerRegistry.controller()

val Player.tumblingPlayer: TumblingPlayer
    get() {
        return playerController.players.find { it.uuid == this.uniqueId }!!
    }

val Player.uiController: PlayerUIController
    get() {
        return playerController.playerUIControllers[this]!!
    }

val Player.formattedName: Component
    get() {
        return Format.formatPlayerName(this.tumblingPlayer)
    }

fun Player.openHandledInventory(id: String) {
    playerController.playerUIControllers[this]!!.openInventory(id)
}

fun Player.enableBossBar(id: String) {
    playerController.playerUIControllers[this]!!.enableBossBar(id)
}

fun Player.disableBossBar(id: String) {
    playerController.playerUIControllers[this]!!.disableBossBar(id)
}

fun Player.activateScoreboard(id: String) {
    DebugUtil.info("Activating score board $id for $name")
    playerController.playerUIControllers[this]!!.activateScoreboard(id)
}

fun Player.deactivateScoreboard(id: String) {
    DebugUtil.info("Deactivating score board $id for $name")
    playerController.playerUIControllers[this]!!.deactivateScoreboard(id)
}


fun Player.hunger() {
    addPotionEffect(PotionEffect(PotionEffectType.HUNGER, PotionEffect.INFINITE_DURATION, 0, true, false, false))
}

fun Player.giveKit(kit: Kit.KitDefinition) {
    Kit.giveKit(this, kit)
}

fun Player.tp(location: Location) {
    // isn't needed for same-dimension teleports
    if(this.location.world == location.world) {
        this.teleport(location)
        return
    }

    playerController.removeNametag(this)
    this.teleport(location)
    playerController.reloadNametag(this)
}

val teleportingPlayers: ArrayList<Player> = ArrayList()
fun Player.fadeTp(location: Location, force: Boolean = false) {
    if(this in teleportingPlayers && !force) return
    teleportingPlayers.add(this)

    val fade = 4
    val title = Title.title(
        Component.text("\uE000").font(NamespacedKey(TreeTumblers.NAMESPACE, "hud")),
        Component.empty(),
        Title.Times.times(Tick.of(fade.toLong()), Tick.of(3), Tick.of(fade.toLong()))
    )

    this.showTitle(title)
    runTaskLater(fade.toLong()) {
        this.tp(location)
        teleportingPlayers.remove(player)
    }
}

// closest i can get to getting you NOT to use this method!
@Deprecated(
    "Player teleport does not work with our custom nametag system, use Player.tp instead",
    level = DeprecationLevel.ERROR
)
@Suppress("UnusedReceiverParameter")
fun Player.teleport() {
}

fun Location.toCenterXZLocation(): Location {
    val center = this.toCenterLocation()
    center.y = this.y
    return center
}

fun List<Location>.getPlayers(heightUp: Int, heightDown: Int, condition: ((player: Player) -> Boolean)? = null): List<Player> {
    return Bukkit.getOnlinePlayers().filter { condition?.invoke(it) ?: true }.filter { player ->
        val playerLocation = player.location

        any { location ->
            location.world == playerLocation.world &&
                playerLocation.blockX == location.blockX &&
                playerLocation.blockZ == location.blockZ &&
                playerLocation.blockY in (location.blockY - heightDown)..(location.blockY + heightUp)
        }
    }
}

fun runTask(runnable: Runnable) =
    Bukkit.getScheduler().runTask(TreeTumblers.plugin, runnable)
fun runTaskLater(delay: Long, runnable: Runnable) =
    Bukkit.getScheduler().runTaskLater(TreeTumblers.plugin, runnable, delay)
fun runTaskTimer(delay: Long, period: Long, runnable: Runnable) =
    Bukkit.getScheduler().runTaskTimer(TreeTumblers.plugin, runnable, delay, period)
fun runTaskAsynchronously(runnable: Runnable) =
    Bukkit.getScheduler().runTaskAsynchronously(TreeTumblers.plugin, runnable)

fun List<Double>.unpackCoordinates(world: World): Location {
    return Location(
        world,
        this[0],
        this[1],
        this[2],
        getOrNull(3)?.toFloat() ?: 0f,
        getOrNull(4)?.toFloat() ?: 0f
    )
}

inline fun <reified T> List<*>.validateList(): List<T>? {
    if (!all { it is T }) return null
    return map { it as T }
}

fun List<*>.validateLocation(world: World): Location? {
    val list = this.map {
        if(it !is Number) return@validateLocation null
        it.toDouble()
    }

    return list.unpackCoordinates(world)
}

fun Location.isInRegion(bound1: Location, bound2: Location): Boolean {
    return this.blockX >= min(bound1.blockX, bound2.blockX) && this.blockY >= min(bound1.blockY, bound2.blockY)
        && this.blockZ >= min(bound1.blockZ, bound2.blockZ) && this.blockX <= max(bound1.blockX, bound2.blockX)
        && this.blockY <= max(bound1.blockY, bound2.blockY) && this.blockZ <= max(bound1.blockY, bound2.blockY)
}

fun Location.forEachRegion(other: Location, execute: (block: Block) -> Unit) {
    for(x in min(this.x, other.x).toInt()..max(this.x, other.x).toInt())
    for(y in min(this.y, other.y).toInt()..max(this.y, other.y).toInt())
    for(z in min(this.z, other.z).toInt()..max(this.z, other.z).toInt()) {
        execute(this.world.getBlockAt(x, y, z))
    }
}

fun Location.toBlockVector3(): BlockVector3 {
    return BlockVector3.at(this.x, this.y, this.z)
}

fun Location.randomBetween(other: Location): Location {
    val x = (min(this.x.toInt(), other.x.toInt())..max(this.x.toInt(), other.x.toInt())).random()
    val y = (min(this.y.toInt(), other.y.toInt())..max(this.y.toInt(), other.y.toInt())).random()
    val z = (min(this.z.toInt(), other.z.toInt())..max(this.z.toInt(), other.z.toInt())).random()

    return Location(this.world, x.toDouble(), y.toDouble(), z.toDouble())
}

fun Location.minOf(other: Location): Location {
    return Location(this.world, min(this.x, other.x), min(this.y, other.y), min(this.z, other.z))
}

fun Location.maxOf(other: Location): Location {
    return Location(this.world, max(this.x, other.x), max(this.y, other.y), max(this.z, other.z))
}

fun World.fill(location1: Location, location2: Location, material: Material) {
    val xRange = (min(location1.x.toInt(), location2.x.toInt())..max(location1.x.toInt(), location2.x.toInt()))
    val yRange = (min(location1.y.toInt(), location2.y.toInt())..max(location1.y.toInt(), location2.y.toInt()))
    val zRange = (min(location1.z.toInt(), location2.z.toInt())..max(location1.z.toInt(), location2.z.toInt()))

    xRange.forEach { x ->
        yRange.forEach { y ->
            zRange.forEach { z ->
                this.getBlockAt(x, y, z).type = material
            }
        }
    }
}

val Long.tickSeconds: Double
    get() {
        return ((this / 20.0) * 10.0).roundToInt() / 10.0
    }

fun Player.buttonClickSound() {
    player!!.playSound(player!!.location, Sound.UI_BUTTON_CLICK, 10f, 1f)
}

fun Player.sound(sound: Sound) {
    player!!.playSound(player!!.location, sound, 10f, 1f)
}

fun Player.sound(sound: String) {
    player!!.playSound(player!!.location, sound, 10f, 1f)
}

fun Player.hideToAll() {
    playerController.hiddenPlayers.add(this)
    Bukkit.getOnlinePlayers().forEach {
        if(it !== this) {
            it.hidePlayer(TreeTumblers.plugin, this)
        }
    }
}

fun Player.showToAll() {
    playerController.hiddenPlayers.remove(this)
    Bukkit.getOnlinePlayers().forEach {
        if(it !== this) {
            it.showPlayer(TreeTumblers.plugin, this)
        }
    }
}

fun announceKill(killer: Player, killed: Player, score: Int? = null) {
    val source = killed.lastDamageCause
    var icon: String? = null

    if(source is EntityDamageByEntityEvent) {
        icon = when (source.damager) {
            is Player -> "\uD83D\uDDE1"

            is Arrow,
            is Firework -> "\uD83C\uDFF9"

            is Trident -> "\uD83D\uDD31"
            is TNTPrimed -> "\uD83D\uDCA3"

            else -> null
        }
    }

    killer.showTitle(Title.title(
        Component.empty(),
        Format.mm("<red>${icon ?: "\uD83D\uDDE1"}</red> <player>${if(score != null) " <gold>[+${score}]</gold>" else ""}", Placeholder.component("player", Format.formatPlayerName(killed))),
        Title.Times.times(Tick.of(0), Tick.of(45), Tick.of(5))
    ))
}

suspend fun <T> suspendSync(task: () -> T): T = withContext(BukkitDispatcher) {
    task.invoke()
}

fun spawnFirework(location: Location, effect: FireworkEffect, detonationDelay: Long = 2L) {
    val world = location.world

    val firework = world.spawn(location, Firework::class.java) { fw ->
        fw.fireworkMeta = fw.fireworkMeta.apply {
            addEffect(effect)
            power = 0
        }
    }

    runTaskLater(detonationDelay) {
        firework.detonate()
    }
}

fun spawnFirework(player: Player, effect: FireworkEffect, detonationDelay: Long = 2L) =
    spawnFirework(player.location.clone(), effect, detonationDelay)

object VoidGenerator : ChunkGenerator() {
    override fun shouldGenerateNoise(): Boolean {
        return false
    }

    override fun shouldGenerateSurface(): Boolean {
        return false
    }

    override fun shouldGenerateCaves(): Boolean {
        return false
    }

    override fun shouldGenerateDecorations(): Boolean {
        return false
    }

    override fun shouldGenerateMobs(): Boolean {
        return false
    }

    override fun shouldGenerateStructures(): Boolean {
        return false
    }

    override fun getDefaultBiomeProvider(worldInfo: WorldInfo): BiomeProvider {
        return object : BiomeProvider() {
            override fun getBiome(
                worldInfo: WorldInfo,
                x: Int,
                y: Int,
                z: Int
            ): Biome {
                return Biome.PLAINS
            }

            override fun getBiomes(worldInfo: WorldInfo): List<Biome> {
                return listOf(Biome.PLAINS)
            }
        }
    }
}

private val values = listOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
private val symbols = listOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")

fun intToRoman(num: Int): String {
    var result = ""
    var n = num
    for (i in values.indices) {
        while (n >= values[i]) {
            n -= values[i]
            result += symbols[i]
        }
    }
    return result
}

fun wrapComponent(component: Component, width: Int): List<Component> {
    val text = PlainTextComponentSerializer.plainText().serialize(component)
    val style = component.style()

    val words = text.split(" ")
    val lines = mutableListOf<Component>()

    var current = StringBuilder()

    for (word in words) {
        if (current.length + word.length + 1 > width) {
            lines += Component.text(current.toString()).style(style)
            current = StringBuilder(word)
        } else {
            if (current.isNotEmpty()) current.append(" ")
            current.append(word)
        }
    }

    if (current.isNotEmpty()) {
        lines += Component.text(current.toString()).style(style)
    }

    return lines
}

fun isArmor(item: ItemStack): Boolean {
    return when (item.type.equipmentSlot) {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET -> true
        else -> false
    }
}

fun formatToMSS(seconds: Int): String {
    val duration = Duration.ofSeconds(seconds.toLong())
    val totalSeconds = duration.seconds
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

fun formatMsTime(ms: Long): String {
    val minutes: Long = ms / 1000 / 60
    val seconds: Long = (ms / 1000) % 60
    val millis: Long = ms % 1000
    return String.format("%d:%02d.%03d", minutes, seconds, millis)
}

fun getOrdinalSuffix(num: Int): String {
    if (num % 100 in 11..13) {
        return "th"
    }
    return when (num % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

fun <T> calculatePlacements(sortedList: List<MutableMap.MutableEntry<T, Int>>): ArrayList<Pair<T, Int>> {
    val placements = ArrayList<Pair<T, Int>>()
    var placement = 1
    var lastScore: Int? = null

    sortedList.forEachIndexed { index, (player, score) ->
        if (lastScore != null && score != lastScore) {
            placement = index + 1
        }
        placements.add(player to placement)
        lastScore = score
    }

    return placements
}

// ai code
// this is magic
fun getEquidistantPoints(center: Location, radius: Double, count: Int): List<Location> {
    return (0 until count).map { i ->
        val angle = (2 * Math.PI * i) / count  // evenly spaced radians
        val x = center.x + radius * cos(angle)
        val z = center.z + radius * sin(angle)
        Location(center.world, x, center.y, z)
    }
}

fun addScoreboardObjectiveLines(objective: Objective, lines: ArrayList<Component>): ArrayList<Score> {
    val scores: ArrayList<Score> = ArrayList()
    lines.forEachIndexed { index, text ->
        val score = objective.getScore("line$index")
        score.customName(text)
        score.score = lines.size - index
        scores.add(score)
    }

    return scores
}

suspend fun titleCountdown(audience: Audience, subtitle: Component, length: Int) {
    repeat(length) {
        val color = when(length - it) {
            3 -> NamedTextColor.GREEN
            2 -> NamedTextColor.YELLOW
            1 -> NamedTextColor.RED
            else -> NamedTextColor.WHITE
        }

        val title = Title.title(
            Component.text("> ${length - it} <", color).decoration(TextDecoration.BOLD, true),
            subtitle,
            Title.Times.times(Tick.of(0), Tick.of(25), Tick.of(0))
        )

        audience.showTitle(title)
        delay(1000)
    }
}

suspend fun subtitleCountdown(audience: Audience, title: Component, length: Int) {
    repeat(length) {
        val color = when(length - it) {
            3 -> NamedTextColor.GREEN
            2 -> NamedTextColor.YELLOW
            1 -> NamedTextColor.RED
            else -> NamedTextColor.WHITE
        }

        val title = Title.title(
            title,
            Component.text("> ${length - it} <", color).decoration(TextDecoration.BOLD, true),
            Title.Times.times(Tick.of(0), Tick.of(25), Tick.of(0))
        )

        audience.showTitle(title)
        delay(1000)
    }
}