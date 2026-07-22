package xyz.devcmb.tumblers.controllers.games.tower_ascent.rooms

import org.bukkit.event.Listener
import xyz.devcmb.tumblers.controllers.games.tower_ascent.feature.TowerGenerator
import xyz.devcmb.tumblers.controllers.games.tower_ascent.feature.TowerHandler

interface RoomController : Listener {
    val noDefaultBehavior: Boolean
        get() = false

    var room: TowerGenerator.LoadedRoom
    var handler: TowerHandler

    /** Called right after the [TowerHandler] has been created **/
    fun load()
    /** Called when players are teleported into the starting elevator */
    fun teleport()
    /** Called when the mobs start spawning */
    fun start()
    /** Called when players are teleported to the next room */
    fun cleanup()
}