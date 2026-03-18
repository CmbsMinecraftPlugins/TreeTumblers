package xyz.devcmb.tumblers.util

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.block.Biome
import org.bukkit.generator.BiomeProvider
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.TreeTumblers
import java.time.Duration
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    fun wrapString(string: String, length: Int): String {
        var wrapCheck = 0
        val wrappedString = StringBuilder()
        for (i in 0..<string.length) {
            if (wrapCheck >= length && string[i] == ' ') {
                wrappedString.append("\n")
                wrapCheck = 0
            } else {
                wrappedString.append(string[i])
            }
            wrapCheck++
        }

        return wrappedString.toString()
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

    // Source - https://stackoverflow.com/a/73494554
    // Posted by SecretX, modified by community. See post 'Timeline' for change history
    // Retrieved 2026-03-05, License - CC BY-SA 4.0
    suspend fun <T> suspendSync(task: () -> T): T = withTimeout(10000L) {
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


