package xyz.devcmb.tumblers.controllers.games.deathrun

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection

interface Trap {
    // Constructor params
    val deathrunController: DeathrunController
    val data: ConfigurationSection
    val from: Location
    val to: Location

    // Everything else
    val name: Component

    val id: String
    val itemKey: NamespacedKey

    suspend fun activate()
}