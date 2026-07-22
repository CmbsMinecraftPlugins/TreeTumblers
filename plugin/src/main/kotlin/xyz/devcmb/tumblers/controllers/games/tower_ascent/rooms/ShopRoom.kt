package xyz.devcmb.tumblers.controllers.games.tower_ascent.rooms

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import xyz.devcmb.tumblers.controllers.games.tower_ascent.TowerAscentController
import xyz.devcmb.tumblers.controllers.games.tower_ascent.feature.TowerGenerator
import xyz.devcmb.tumblers.controllers.games.tower_ascent.feature.TowerHandler
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.forEachRegion

class ShopRoom : RoomController {
    override val noDefaultBehavior: Boolean = true
    override lateinit var handler: TowerHandler
    override lateinit var room: TowerGenerator.LoadedRoom

    val elevatorBlocks: ArrayList<Location> = ArrayList()

    override fun load() {
        room.endingElevatorBounds.first.forEachRegion(room.endingElevatorBounds.second) {
            if(it.type == Material.IRON_BLOCK) {
                it.type = Material.AIR
                elevatorBlocks.add(it.location)
            }
        }
    }

    override fun teleport() {
        handler.elevatorOpen = true
        handler.elevatorBlocks.addAll(elevatorBlocks)

        Bukkit.broadcast(handler.controller.gameMessage(Format.mm(
            "<yellow><team> have arrived at a shop at room <white>${handler.currentRoom}</white></yellow>",
            Placeholder.component("team", handler.team.formattedName)
        )))
    }

    override fun start() {
    }

    override fun cleanup() {
    }
}