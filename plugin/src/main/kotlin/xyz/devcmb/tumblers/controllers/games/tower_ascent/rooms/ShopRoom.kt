package xyz.devcmb.tumblers.controllers.games.tower_ascent.rooms

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.noxcrew.noxesium.core.registry.CommonEntityComponentTypes
import com.noxcrew.noxesium.paper.component.setNoxesiumComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.fui.draw.TextDrawContext
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.tower_ascent.feature.TowerGenerator
import xyz.devcmb.tumblers.controllers.games.tower_ascent.feature.TowerHandler
import xyz.devcmb.tumblers.controllers.player.UIController
import xyz.devcmb.tumblers.item.advanced.AdvancedItemStack
import xyz.devcmb.tumblers.item.custom.scroll.ScrollItem
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.isInRegion
import xyz.devcmb.tumblers.util.ticks
import xyz.devcmb.tumblers.util.tumblingPlayer
import java.awt.Color

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
                it.setNoxesiumComponent(CommonEntityComponentTypes.GLOW_COLOR, Color(0, 255, 0))
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
                        5.0,
                        0.5
                    ) { entity -> entity is ItemDisplay }

                    val entity = result?.hitEntity as? ItemDisplay
                    if(entity == null) {
                        if(playerCurrentShopItems.contains(it)) {
                            val shopItem = shopItems[playerCurrentShopItems[it]!!]
                            unGlow(it, shopItem.entity)
                            it.showTitle(Title.title(Component.empty(), Component.empty()))
                        }

                        playerCurrentShopItems.remove(it)
                        return@forEach
                    }

                    val shopItem = shopItems.find { entry -> entry.entity == entity } ?: return@forEach
                    sendItemTitle(it, shopItem)
                    if(playerCurrentShopItems[it] == shopItems.indexOf(shopItem)) return@forEach

                    val previousShopItem = shopItems.getOrNull(playerCurrentShopItems.getOrElse(it) { -1 })
                    playerCurrentShopItems[it] = shopItems.indexOf(shopItem)

                    // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Entity_Metadata
                    val entityMetadataPacket = WrapperPlayServerEntityMetadata(
                        shopItem.entity.entityId,
                        listOf(EntityData(0, EntityDataTypes.BYTE, 0x40))
                    )
                    PacketEvents.getAPI().playerManager.sendPacket(it, entityMetadataPacket)

                    if(previousShopItem != null) {
                        unGlow(it, previousShopItem.entity)
                    }
                }
            }
        }
        shopDisplayTask!!.runTaskTimer(TreeTumblers.plugin, 0, 5)
    }

    private fun unGlow(player: Player, entity: Entity) {
        val previousItemMetadataPacket = WrapperPlayServerEntityMetadata(
            entity.entityId,
            listOf(EntityData(0, EntityDataTypes.BYTE, 0x00))
        )
        PacketEvents.getAPI().playerManager.sendPacket(player, previousItemMetadataPacket)
    }

    private fun sendItemTitle(player: Player, item: PurchasableShopItem) {
        player.showTitle(Title.title(
            Component.empty(),
            UIController.fUI.draw(200) { ctx ->
                val itemName = PlainTextComponentSerializer.plainText().serialize(item.entity.itemStack.effectiveName())
                val priceComponent = Format.mm(
                    "<white>$itemName</white> <dark_gray>-</dark_gray> <gold>${item.shopItem.price}</gold><sprite:items:item/gold_ingot>",
                )
                ctx.drawAlignedWithWidth(
                    priceComponent,
                    UIController.fUI.fontMeasurer.measureComponent(priceComponent) + 8,
                    TextDrawContext.Alignment.CENTER.alignmentConstant
                )
                ctx.moveCursor(0, 8)
                ctx.drawAligned(Format.mm("<gray>Right-Click to Buy</gray>"), TextDrawContext.Alignment.CENTER)
            },
            Title.Times.times(0.ticks, 10.ticks, 0.ticks)
        ))
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
        SPEED_SCROLL(30, ScrollItem(ScrollItem.ScrollEffect.SPEED).build())
    }

    val cooldowns: HashMap<Player, Long> = HashMap()
    @EventHandler
    fun playerInteractEvent(event: PlayerInteractEvent) {
        val player = event.player
        if(cooldowns[player] != null && cooldowns[player]!! > System.currentTimeMillis()) return
        if(event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) return
        val currentShopItemIndex = playerCurrentShopItems[player] ?: return
        val shopItem = shopItems[currentShopItemIndex]

        cooldowns[player] = System.currentTimeMillis() + 500

        val playerGoldCount = (handler.controller.playerGoldCounts[player.tumblingPlayer] ?: 0)
        if(playerGoldCount < shopItem.shopItem.price) {
            player.sendMessage(Format.error("You cannot afford this item!"))
            return
        }

        handler.controller.playerGoldCounts[player.tumblingPlayer] = playerGoldCount - shopItem.shopItem.price
        player.showTitle(Title.title(
            Component.empty(),
            Format.mm("<gold>[-${shopItem.shopItem.price}<white><sprite:items:item/gold_ingot></white>]</gold>"),
            Title.Times.times(5.ticks, 30.ticks, 5.ticks)
        ))

        val builtItem = shopItem.shopItem.item.build()
        player.inventory.addItem(builtItem)

        shopItem.entity.remove()
        shopItems.remove(shopItem)

        playerCurrentShopItems.filter { it.value == currentShopItemIndex }.forEach { _ -> playerCurrentShopItems.remove(player) }
    }

}