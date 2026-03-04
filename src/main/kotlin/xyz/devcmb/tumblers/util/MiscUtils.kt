package xyz.devcmb.tumblers.util

import org.bukkit.block.Biome
import org.bukkit.generator.BiomeProvider
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo

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
}