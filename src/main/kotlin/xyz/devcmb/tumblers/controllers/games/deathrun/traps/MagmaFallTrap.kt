package xyz.devcmb.tumblers.controllers.games.deathrun.traps

import org.bukkit.configuration.ConfigurationSection

class MagmaFallTrap(
    val data: ConfigurationSection
) : Trap {
    override val id: String = "magma_fall"
    override fun activate() {

    }
}