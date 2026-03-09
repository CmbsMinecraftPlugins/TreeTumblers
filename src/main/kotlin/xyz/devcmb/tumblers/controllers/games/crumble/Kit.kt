package xyz.devcmb.tumblers.controllers.games.crumble

import org.bukkit.NamespacedKey

interface Kit {
    val id: String
    val name: String
    val inventoryModel: NamespacedKey
}