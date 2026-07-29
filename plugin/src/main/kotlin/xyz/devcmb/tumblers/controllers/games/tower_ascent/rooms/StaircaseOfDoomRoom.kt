package xyz.devcmb.tumblers.controllers.games.tower_ascent.rooms

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import xyz.devcmb.tumblers.controllers.games.tower_ascent.feature.TowerGenerator
import xyz.devcmb.tumblers.controllers.games.tower_ascent.feature.TowerHandler
import xyz.devcmb.tumblers.util.isInRegion

class StaircaseOfDoomRoom(val roomIndex: Int) : RoomController {
    override lateinit var handler: TowerHandler
    override lateinit var room: TowerGenerator.LoadedRoom

    /** Called right after the [TowerHandler] has been created **/
    override fun load() {
    }

    /** Called when players are teleported into the starting elevator */
    override fun teleport() {
    }

    /** Called when the mobs start spawning */
    override fun start() {
    }

    /** Called when players are teleported to the next room */
    override fun cleanup(gameOver: Boolean) {
    }

    @EventHandler
    fun playerInteractEvent(event: PlayerInteractEvent) {
        if(event.action == Action.RIGHT_CLICK_BLOCK) {
            val block = event.clickedBlock ?: return
            val currentItem = event.item ?: return

            if(
                block.type == Material.DEEPSLATE_EMERALD_ORE
                && block.location.isInRegion(room.roomBounds.first, room.roomBounds.second)
                && currentItem.persistentDataContainer.get(ShopRoom.rustedKey, PersistentDataType.BOOLEAN) == true
            ) {
                block.type = Material.AIR
                removeAdjacentBlocks(block)
                currentItem.amount -= 1
            }
        }
    }

    fun removeAdjacentBlocks(block: Block) {
        BlockFace.entries.forEach {
            val relative = block.getRelative(it)
            if("COPPER" in relative.type.name) {
                relative.type = Material.AIR
                removeAdjacentBlocks(relative)
            }
        }
    }
}