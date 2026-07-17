package xyz.devcmb.tumblers.util

import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.world.block.BlockType
import com.sk89q.worldedit.world.block.BlockTypes
import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.configuration.MemorySection
import org.bukkit.entity.Firework
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.generator.BiomeProvider
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Score
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.TumblingConfigKeyMissingException
import xyz.devcmb.tumblers.TumblingConfigTypeMismatchException
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.player.NametagController
import xyz.devcmb.tumblers.controllers.player.PlayerController
import xyz.devcmb.tumblers.data.TumblingPlayer
import java.time.Duration
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import xyz.devcmb.tumblers.item.Kit

/** Gets a [TumblingPlayer] for the attached [Player] **/
val Player.tumblingPlayer: TumblingPlayer
    get() {
        return PlayerController.players.find { it.uuid == this.uniqueId }!!
    }

/** Gets the formatted player name from the [Format.formatPlayerName] method **/
val Player.formattedName: Component
    get() {
        return Format.formatPlayerName(this.tumblingPlayer)
    }

/** Opens a [xyz.devcmb.tumblers.ui.inventory.HandledInventory] by its ID **/
fun Player.openHandledInventory(id: String) {
    PlayerController.playerUIControllers[this]!!.openInventory(id)
}

/** Hides a player and their nametag **/
fun Player.hidePlayerAndTag(other: Player) {
    this.hidePlayer(TreeTumblers.plugin, other)
    NametagController.playerTags[other]?.let {
        val entities = listOf(it.nameTag, it.healthBar)
        entities.forEach { e -> this.hideEntity(TreeTumblers.plugin, e) }
    }
}

/** Shows a player and their nametag **/
fun Player.showPlayerAndTag(other: Player) {
    this.showPlayer(TreeTumblers.plugin, other)
    NametagController.playerTags[other]?.let {
        val entities = listOf(it.nameTag, it.healthBar)
        entities.forEach { e -> this.showEntity(TreeTumblers.plugin, e) }
    }
}

/** Enables a [xyz.devcmb.tumblers.ui.bossbar.HandledBossbar] by its ID **/
fun TumblingPlayer.enableBossBar(id: String) {
    this.currentBossbars.add(id)
    this.bukkitPlayer?.let {
        PlayerController.playerUIControllers[it]!!.enableBossBar(id)
    }
}

/** Disables a [xyz.devcmb.tumblers.ui.bossbar.HandledBossbar] by its ID **/
fun TumblingPlayer.disableBossBar(id: String) {
    this.currentBossbars.remove(id)
    this.bukkitPlayer?.let {
        PlayerController.playerUIControllers[it]!!.disableBossBar(id)
    }
}

/** Activates a [xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard] by its ID **/
fun TumblingPlayer.activateScoreboard(id: String) {
    this.currentScoreboards.add(id)
    this.bukkitPlayer?.let {
        PlayerController.playerUIControllers[it]!!.activateScoreboard(id)
    }
}

/** Deactivates a [xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard] by its ID **/
fun TumblingPlayer.deactivateScoreboard(id: String) {
    this.currentScoreboards.remove(id)
    this.bukkitPlayer?.let {
        PlayerController.playerUIControllers[it]!!.deactivateScoreboard(id)
    }
}

/** Enable a [xyz.devcmb.tumblers.ui.actionbar.HandledActionBar] by its ID **/
fun TumblingPlayer.enableActionBar(id: String) {
    this.currentActionBars.add(id)
    this.bukkitPlayer?.let {
        PlayerController.playerUIControllers[it]!!.enableActionBar(id)
    }
}

/** Disable a [xyz.devcmb.tumblers.ui.actionbar.HandledActionBar] by its ID **/
fun TumblingPlayer.disableActionBar(id: String) {
    this.currentActionBars.remove(id)
    this.bukkitPlayer?.let {
        PlayerController.playerUIControllers[it]!!.disableActionBar(id)
    }
}

/** Enable a [xyz.devcmb.tumblers.ui.actionbar.HandledActionBar] on the [TumblingPlayer] of the attached player by its ID **/
fun Player.enableActionBar(id: String) = this.tumblingPlayer.enableActionBar(id)

/** Disable a [xyz.devcmb.tumblers.ui.actionbar.HandledActionBar] on the [TumblingPlayer] of the attached player by its ID **/
fun Player.disableActionBar(id: String) = this.tumblingPlayer.disableActionBar(id)

/** Give a player all items in a [Kit.KitDefinition] **/
fun Player.giveKit(kit: Kit.KitDefinition) {
    Kit.giveKit(this, kit)
}

/** Teleports a player and their nametag to a location **/
fun Player.tp(location: Location) {
    // isn't needed for same-dimension teleports
    if(this.location.world == location.world) {
        this.teleport(location)
        return
    }

    NametagController.removePlayerTags(this)
    this.teleport(location)
    NametagController.createPlayerTags(this)
}

private val teleportingPlayers: ArrayList<Player> = ArrayList()
/** Teleports a player to a location with a fade of 4 ticks up, 3 ticks stay, and 4 ticks down **/
fun Player.fadeTp(location: Location, force: Boolean = false) {
    if(this in teleportingPlayers && !force) return
    teleportingPlayers.add(this)

    val fade = 4
    val title = Title.title(
        Font.getGlyph("hud/fade"),
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

/** Converts a [Location] to one where the X and Z components are centered on the block **/
fun Location.toCenterXZLocation(): Location {
    val center = this.toCenterLocation()
    center.y = this.y
    return center
}

/** Gets all players that are the specified locations from [heightDown] to [heightUp] if the [condition] is true (if provided) **/
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

/** Unpack a list of doubles into a location **/
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

/** Valides all the elements of a list are of type [T] **/
inline fun <reified T> List<*>.validateList(): List<T>? {
    if (!all { it is T }) return null
    return map { it as T }
}

/** Converts a list of [Number] into a location assuming it is valid and resolves to a valid location **/
fun List<*>.validateLocation(world: World): Location? {
    val list = this.map {
        if(it !is Number) return@validateLocation null
        it.toDouble()
    }

    return list.unpackCoordinates(world)
}

/** Checks if certain values in a hashmap are valid given a condition */
fun HashMap<*, *>.validateElements(values: HashMap<*, ((param: Any) -> Boolean)>): Boolean {
    values.forEach { (key, validate) ->
        if(this[key] == null || !validate.invoke(this[key]!!)) return@validateElements false
    }

    return true
}

/** Checks if the attached [Location] is inside the boundaries [bound1] and [bound2] **/
fun Location.isInRegion(bound1: Location, bound2: Location): Boolean {
    return this.blockX >= min(bound1.blockX, bound2.blockX) && this.blockY >= min(bound1.blockY, bound2.blockY)
        && this.blockZ >= min(bound1.blockZ, bound2.blockZ) && this.blockX <= max(bound1.blockX, bound2.blockX)
        && this.blockY <= max(bound1.blockY, bound2.blockY) && this.blockZ <= max(bound1.blockZ, bound2.blockZ)
}

/** Executes lambda [execute] on every location in between [this] and [other] **/
fun Location.forEachRegion(other: Location, execute: (block: Block) -> Unit) {
    for(x in min(this.x, other.x).toInt()..max(this.x, other.x).toInt())
    for(y in min(this.y, other.y).toInt()..max(this.y, other.y).toInt())
    for(z in min(this.z, other.z).toInt()..max(this.z, other.z).toInt()) {
        execute(this.world.getBlockAt(x, y, z))
    }
}

/** Returns [this] with the Y value set to [y] **/
fun Location.withY(y: Double): Location {
    val loc = this.clone()
    loc.y = y
    return loc
}

/** Converts the location to a [BlockVector3] **/
fun Location.toBlockVector3(): BlockVector3 {
    return BlockVector3.at(this.x, this.y, this.z)
}

/** Picks a random location between [this] and [other] **/
fun Location.randomBetween(other: Location): Location {
    val x = (min(this.x.toInt(), other.x.toInt())..max(this.x.toInt(), other.x.toInt())).random()
    val y = (min(this.y.toInt(), other.y.toInt())..max(this.y.toInt(), other.y.toInt())).random()
    val z = (min(this.z.toInt(), other.z.toInt())..max(this.z.toInt(), other.z.toInt())).random()

    return Location(this.world, x.toDouble(), y.toDouble(), z.toDouble())
}

/** Gets the minimum location of [this] and [other] **/
fun Location.minOf(other: Location): Location {
    return Location(this.world, min(this.x, other.x), min(this.y, other.y), min(this.z, other.z))
}

/** Gets the maximum location of [this] and [other] **/
fun Location.maxOf(other: Location): Location {
    return Location(this.world, max(this.x, other.x), max(this.y, other.y), max(this.z, other.z))
}

/** Fill from [location1] to [location2] with [material] **/
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

/** Get the amount of seconds in [this] amount of ticks **/
val Long.tickSeconds: Double
    get() {
        return ((this / 20.0) * 10.0).roundToInt() / 10.0
    }

/** Plays a [Sound.UI_BUTTON_CLICK] sound effect **/
fun Player.buttonClickSound() {
    player!!.playSound(player!!.location, Sound.UI_BUTTON_CLICK, 10f, 1f)
}

/** Playes [sound] to the player **/
fun Player.sound(sound: Sound) {
    player!!.playSound(player!!.location, sound, 10f, 1f)
}

/** Hides a player to all players **/
fun Player.hideToAll() {
    PlayerController.hiddenPlayers.add(this)
    Bukkit.getOnlinePlayers().forEach {
        if(it !== this) {
            it.hidePlayerAndTag(this)
        }
    }
}

/** Shows a player to all players **/
fun Player.showToAll() {
    PlayerController.hiddenPlayers.remove(this)
    Bukkit.getOnlinePlayers().forEach {
        if(it !== this) {
            it.showPlayerAndTag(this)
        }
    }
}

/** Brings a suspending function call back onto the main bukkit thread to execute [task] **/
suspend fun <T> suspendSync(task: () -> T): T = withContext(BukkitDispatcher) {
    task.invoke()
}

/** Spawns a firework at [location] with the firework effect [effect], and a detonation delay of [detonationDelay] **/
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

/** Spawns a firework at the [player]'s location with the firework effect [effect], and a detonation delay of [detonationDelay] **/
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

/** Converts an integer to roman numerals **/
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

/** Wraps a text component onto multiple lines **/
// FIXME: This doesn't preserve the styling of the component, and never has
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

/** Check if a given [item] is an armor piece */
fun isArmor(item: ItemStack): Boolean {
    return when (item.type.equipmentSlot) {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET -> true
        else -> false
    }
}

/** Formats [seconds] in the format M:SS **/
fun formatToMSS(seconds: Int): String {
    val duration = Duration.ofSeconds(seconds.toLong())
    val totalSeconds = duration.seconds
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

/** Formats [ms] in the format M:SS.mmm **/
fun formatMsTime(ms: Long): String {
    val minutes: Long = ms / 1000 / 60
    val seconds: Long = (ms / 1000) % 60
    val millis: Long = ms % 1000
    return String.format("%d:%02d.%03d", minutes, seconds, millis)
}

/** Gets the ordinal suffix for [num] **/
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

/** Calculates placements of a sorted list including ties **/
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

/** Gets a random point along a circle's circumfrence with a [radius] and [center] */
fun getRandomCirclePoint(center: Location, radius: Double): Location {
    val angle = Random.nextDouble(0.0, Math.PI * 2)
    return Location(
        center.world,
        center.x + radius * cos(angle),
        center.y,
        center.z + radius * sin(angle)
    )
}

/** Add multiple components to an objective */
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

/** Does a title countdown for [audience], starting at [length] and counting down **/
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

/** Does a subtitle countdown for [audience], starting at [length] and counting down **/
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

/** Gets a configurable value of type [T] from [path] */
inline fun <reified T> configurable(path: String): T {
    val cfg = TreeTumblers.plugin.config
    if(!cfg.contains(path)) throw TumblingConfigKeyMissingException(path)
    if(!cfg.isSet(path)) DebugUtil.warning("Config path $path is not set! Defaulting to default value.")

    val value = when(T::class) {
        Int::class -> cfg.getInt(path)
        Long::class -> cfg.getLong(path)
        Double::class -> cfg.getDouble(path)
        Float::class -> cfg.getDouble(path).toFloat()
        String::class -> cfg.getString(path)

        HashMap::class -> cfg.getConfigurationSection(path)
            ?.getKeys(false)
            ?.associateWith { key ->
                cfg.get("$path.$key")
            }
            ?.toMap(HashMap())
            ?: HashMap<String, MemorySection>()

        Material::class -> Material.matchMaterial(cfg.getString(path)!!)

        else -> cfg.get(path) as? T ?: throw TumblingConfigTypeMismatchException(path, T::class.simpleName ?: "None", cfg.get(path)!!::class.simpleName ?: "None")
    }

    return value as T
}

/** Runs [action] for every element in a grid of [rows] and [columns], and provides an index */
fun forEachInGridIndexed(rows: Int, columns: Int, action: (index: Int, row: Int, col: Int) -> Unit) {
    var index = 0
    for(row in 0 until rows) {
        for(column in 0 until columns) {
            action(index, row, column)
            index++
        }
    }
}

/** Converts [loc] to a world [Location] of where it was pasted given a [pasteLocation] */
fun Clipboard.getPostPasteLocation(
    loc: BlockVector3,
    pasteLocation: Location
): Location {
    val origin = this.origin

    val offset = loc.subtract(origin)

    return pasteLocation.clone().add(
        offset.x().toDouble(),
        offset.y().toDouble(),
        offset.z().toDouble()
    )
}

/** Gets the boundaries of a clipboard after it has been pasted into the world at [loadPosition] */
fun Clipboard.getPostPasteBounds(loadPosition: Location): Pair<Location, Location> {
    val nonAirBlocks = region
        .asSequence()
        .filter { getBlock(it).blockType != BlockTypes.AIR }
        .map { BlockVector3.at(it.x(), it.y(), it.z()) }
        .toList()

    val minRegion = BlockVector3.at(
        nonAirBlocks.minOf { it.x() },
        nonAirBlocks.minOf { it.y() },
        nonAirBlocks.minOf { it.z() }
    )

    val maxRegion = BlockVector3.at(
        nonAirBlocks.maxOf { it.x() },
        nonAirBlocks.maxOf { it.y() },
        nonAirBlocks.maxOf { it.z() }
    )

    val offset = BlockVector3.at(
        loadPosition.blockX,
        loadPosition.blockY,
        loadPosition.blockZ
    ).subtract(origin)

    return Pair(
        Location(
            loadPosition.world,
            (minRegion.x() + offset.x()).toDouble(),
            (minRegion.y() + offset.y()).toDouble(),
            (minRegion.z() + offset.z()).toDouble()
        ),
        Location(
            loadPosition.world,
            (maxRegion.x() + offset.x()).toDouble(),
            (maxRegion.y() + offset.y()).toDouble(),
            (maxRegion.z() + offset.z()).toDouble()
        )
    )
}

/** Checks if a playercheck is currently active in a game **/
fun isPlayercheckActive(): Boolean {
    return GameController.activeGame?.playerCheckActive != true
}

/** Gives the splash potion of a [PotionEffect] with the item name being set to [name] */
fun PotionEffect.splashPotion(name: String): ItemStack {
    return ItemStack.of(Material.SPLASH_POTION).apply {
        editMeta(PotionMeta::class.java) { meta ->
            meta.addCustomEffect(this@splashPotion, true)
            meta.color = meta.computeEffectiveColor()
            meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false))
        }
    }
}

/** Checks if an interaction entity overlaps [location] */
fun Interaction.contains(location: Location): Boolean {
    val box = this.boundingBox
    return box.contains(location.toVector())
}

/** Checks if an interaction entity overlaps with the bounding box of [player] **/
fun Interaction.contains(player: Player) = contains(player.location)

/** Gets a location in between a line of [length] blocks of [type] on either the X or Z axis */
fun Clipboard.getPivot(type: BlockType, length: Int = 5): BlockVector3? {
    require(length % 2 == 1) { "Pivot length must be an odd number" }

    val sideLength = (length - 1)/2
    this.region.forEach { origin ->
        if (this.getBlock(origin).blockType != type) return@forEach

        fun check(dx: Int, dz: Int): BlockVector3? {
            for (i in -sideLength..sideLength) {
                val pos = BlockVector3.at(
                    origin.x() + dx * i,
                    origin.y(),
                    origin.z() + dz * i
                )

                if (!this.region.contains(pos)) return null
                if (this.getBlock(pos).blockType != type) return null
            }
            return origin
        }

        val xCheck = check(1,0)
        val zCheck = check(0,1)

        if(xCheck != null || zCheck != null) return xCheck ?: zCheck
    }

    return null
}