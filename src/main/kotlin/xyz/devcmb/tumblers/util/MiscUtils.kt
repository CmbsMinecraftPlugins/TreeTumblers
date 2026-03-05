package xyz.devcmb.tumblers.util

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.bukkit.Bukkit
import org.bukkit.block.Biome
import org.bukkit.generator.BiomeProvider
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import xyz.devcmb.tumblers.TreeTumblers
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object MiscUtils {
    object VoidGenerator : ChunkGenerator() {
        public override fun shouldGenerateNoise(): Boolean {
            return false
        }

        public override fun shouldGenerateSurface(): Boolean {
            return false
        }

        public override fun shouldGenerateCaves(): Boolean {
            return false
        }

        public override fun shouldGenerateDecorations(): Boolean {
            return false
        }

        public override fun shouldGenerateMobs(): Boolean {
            return false
        }

        public override fun shouldGenerateStructures(): Boolean {
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


