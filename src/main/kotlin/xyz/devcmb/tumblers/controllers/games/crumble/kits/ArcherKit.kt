package xyz.devcmb.tumblers.controllers.games.crumble.kits

import org.bukkit.NamespacedKey
import xyz.devcmb.tumblers.controllers.games.crumble.Kit

class ArcherKit(
    override val inventoryModel: NamespacedKey = NamespacedKey("tumbling", "crumble/archer"),
    override val id: String = "archer",
    override val name: String = "Archer"
) : Kit