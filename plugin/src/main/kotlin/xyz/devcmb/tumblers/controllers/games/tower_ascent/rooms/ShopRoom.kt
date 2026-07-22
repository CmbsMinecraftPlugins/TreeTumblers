package xyz.devcmb.tumblers.controllers.games.tower_ascent.rooms

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.tower_ascent.feature.TowerGenerator
import xyz.devcmb.tumblers.controllers.games.tower_ascent.feature.TowerHandler
import xyz.devcmb.tumblers.item.advanced.AdvancedItemStack
import xyz.devcmb.tumblers.item.custom.scroll.ScrollItem
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.isInRegion

class ShopRoom : RoomController {
    override val noDefaultBehavior: Boolean = true
    override lateinit var handler: TowerHandler
    override lateinit var room: TowerGenerator.LoadedRoom

    val elevatorBlocks: ArrayList<Location> = ArrayList()
    val shopItems: ArrayList<PurchasableShopItem> = ArrayList()

    override fun load() {
        room.endingElevatorBounds.first.forEachRegion(room.endingElevatorBounds.second) {
            if(it.type == Material.IRON_BLOCK) {
                it.type = Material.AIR
                elevatorBlocks.add(it.location)
            }
        }

        handler.controller.map.world.entities
            .filter { it.location.isInRegion(room.roomBounds.first, room.roomBounds.second) }
            .filterIsInstance<ItemDisplay>()
            .forEach {
                val shopItem = ShopItem.entries.random()
                val item = shopItem.item.build()

                it.setItemStack(item)
                shopItems.add(PurchasableShopItem(
                    it,
                    shopItem
                ))
            }
    }

    var shopDisplayTask: BukkitRunnable? = null
    val playerCurrentShopItems: HashMap<Player, Int> = HashMap()
    override fun teleport() {
        handler.elevatorOpen = true
        handler.elevatorBlocks.addAll(elevatorBlocks)

        Bukkit.broadcast(handler.controller.gameMessage(Format.mm(
            "<yellow><team> have arrived at a shop at room <white>${handler.currentRoomIndex + 1}</white></yellow>",
            Placeholder.component("team", handler.team.formattedName)
        )))

        shopDisplayTask = object : BukkitRunnable() {
            override fun run() {
                handler.team.getOnlinePlayers().forEach {
                    val result = handler.controller.map.world.rayTraceEntities(
                        it.eyeLocation,
                        it.eyeLocation.direction,
                        30.0,
                        0.2
                    ) { entity -> entity is ItemDisplay }

                    val entity = result?.hitEntity as? ItemDisplay
                    if(entity == null) {
                        playerCurrentShopItems.remove(it)
                        return@forEach
                    }

                    val shopItem = shopItems.find { entry -> entry.entity == entity } ?: return@forEach
                    playerCurrentShopItems[it] = shopItems.indexOf(shopItem)
                }
            }
        }
        shopDisplayTask!!.runTaskTimer(TreeTumblers.plugin, 0, 5)
    }

    override fun start() {
    }

    override fun cleanup() {
        shopDisplayTask?.cancel()
    }

    data class PurchasableShopItem(
        val entity: ItemDisplay,
        val shopItem: ShopItem
    )

    enum class ShopItem(val price: Int, val item: AdvancedItemStack) {
        DIAMOND_SWORD(80, AdvancedItemStack(Material.DIAMOND_SWORD)),
        DIAMOND_LEGGINGS(65, AdvancedItemStack(Material.DIAMOND_LEGGINGS)),
        SPEED_SCROLL(40, ScrollItem(ScrollItem.ScrollEffect.SPEED).build())
    }
}