package xyz.devcmb.tumblers.util

import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.block.Biome
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.generator.BiomeProvider
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.Objective
import xyz.devcmb.tumblers.TreeTumblers
import java.time.Duration
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.cos
import kotlin.math.sin

object MiscUtils {
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
        if (num % 100 >= 11 && num % 100 <= 13) {
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

    fun addScoreboardObjectiveLines(objective: Objective, lines: ArrayList<Component>) {
        lines.forEachIndexed { index, text ->
            val score = objective.getScore("line$index")
            score.customName(text)
            score.score = lines.size - index
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

    fun spawnFirework(player: Player, effect: FireworkEffect, detonationDelay: Long = 2L) {
        val world = player.world
        val location = player.location.clone()

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

    // Source - https://stackoverflow.com/a/73494554
    // Posted by SecretX, modified by community. See post 'Timeline' for change history
    // Retrieved 2026-03-05, License - CC BY-SA 4.0
    suspend fun <T> suspendSync(task: () -> T): T = withTimeout(100000L) {
        // Context: The current coroutine context
        suspendCancellableCoroutine { cont ->
            // Context: The current coroutine context
            Bukkit.getScheduler().runTask(TreeTumblers.plugin, Runnable {
                // Context: Bukkit MAIN thread
                // runCatching is used to forward any exception that may occur here back to
                // our coroutine, keeping the exception transparency of Kotlin coroutines
                runCatching(task).fold({ cont.resume(it) }, cont::resumeWithException)
            })
        }
    }
}


