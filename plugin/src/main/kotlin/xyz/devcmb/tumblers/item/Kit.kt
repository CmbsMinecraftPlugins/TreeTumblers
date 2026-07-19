package xyz.devcmb.tumblers.item

import org.bukkit.Color
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.persistence.PersistentDataType
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.item.advanced.AdvancedItemStack
import xyz.devcmb.tumblers.util.tumblingPlayer
import java.util.UUID

object Kit {
    interface KitDefinition {
        val items: ArrayList<KitItem>
        val defaultDropability: Boolean
            get() = false
        val uuid: UUID
    }

    sealed interface KitItem {
        fun getStack(kit: KitDefinition, player: Player): ItemStack
        fun giveItem(player: Player, item: ItemStack, slot: Int) {
            player.inventory.setItem(slot, item)
        }

        class AdvancedItem(
            val stack: AdvancedItemStack,
            val droppableOverride: Boolean? = null,
        ): KitItem {
            override fun getStack(kit: KitDefinition, player: Player): ItemStack {
                val context = stack.context
                if(!context.droppableChanged) context.droppable(droppableOverride ?: kit.defaultDropability)
                return stack.build()
            }
        }

        class StandardItem(
            val itemStack: ItemStack,
            val droppableOverride: Boolean? = null
        ): KitItem {
            override fun getStack(kit: KitDefinition, player: Player): ItemStack {
                return AdvancedItemStack(itemStack.clone()) {
                    droppable(droppableOverride ?: kit.defaultDropability)
                }.build()
            }
        }

        class ArmorItem(val itemStack: ItemStack): KitItem {
            override fun getStack(kit: KitDefinition, player: Player): ItemStack {
                val item = AdvancedItemStack(itemStack.clone()) {
                    droppable(kit.defaultDropability)
                }.build()

                if(item.type.name.contains("LEATHER")) {
                    item.itemMeta = item.itemMeta.also { meta ->
                        val meta = meta as LeatherArmorMeta
                        val playerTeam = player.tumblingPlayer.team
                        meta.setColor(Color.fromRGB(playerTeam.color.value()))
                    }
                }

                return item
            }

            override fun giveItem(player: Player, item: ItemStack, slot: Int) {
                player.inventory.setItem(item.type.equipmentSlot, item)
            }
        }

        class TeamConcreteItem(val droppableOverride: Boolean? = null) : KitItem {
            override fun getStack(kit: KitDefinition, player: Player): ItemStack {
                return AdvancedItemStack(ItemStack.of(player.tumblingPlayer.team.concrete)) {
                    returnOnPlace = true
                    droppable(droppableOverride ?: kit.defaultDropability)
                    count(64)
                }.build()
            }
        }
    }

    val loadouts: ArrayList<KitLoadout> = ArrayList()
    fun giveKit(player: Player, kit: KitDefinition) {
        player.inventory.clear()

        val loadout = loadouts.find { it.player == player.tumblingPlayer && it.id == kit.uuid }
        val itemSlots: HashMap<Int, Int> = hashMapOf()
        kit.items.forEachIndexed { index, item ->
            val slot = loadout?.loadout?.get(index) ?: index
            val itemStack = item.getStack(kit, player)
            itemStack.apply {
                editMeta {
                    it.persistentDataContainer.set(
                        NamespacedKey(TreeTumblers.NAMESPACE, "kit"),
                        PersistentDataType.STRING,
                        kit.uuid.toString()
                    )

                    it.persistentDataContainer.set(
                        NamespacedKey(TreeTumblers.NAMESPACE, "kit_item_index"),
                        PersistentDataType.INTEGER,
                        index
                    )

                    itemSlots[index] = slot
                }
            }
            item.giveItem(player, itemStack, slot)
        }

        if(loadout == null) {
            loadouts.add(KitLoadout(player.tumblingPlayer, kit.uuid, itemSlots))
        }
    }

    fun updateLoadout(player: Player) {
        val kitID = player.inventory.contents.firstNotNullOfOrNull { item ->
            if(item == null) return@firstNotNullOfOrNull null
            item.persistentDataContainer.get(
                NamespacedKey(TreeTumblers.NAMESPACE, "kit"),
                PersistentDataType.STRING
            ).takeIf { it != null }
        } ?: return
        val kitUUID = UUID.fromString(kitID)

        val currentLayout = loadouts.find { it.player == player.tumblingPlayer && it.id == kitUUID }?.loadout ?: hashMapOf()
        val newLayout: HashMap<Int, Int> = HashMap()

        player.inventory.contents.forEachIndexed { slot, stack ->
            if(stack == null) return@forEachIndexed

            if (
                stack.persistentDataContainer.get(
                    NamespacedKey(TreeTumblers.NAMESPACE, "kit"),
                    PersistentDataType.STRING
                ) != kitID
            ) return@forEachIndexed

            val index = stack.persistentDataContainer.get(
                NamespacedKey(TreeTumblers.NAMESPACE, "kit_item_index"),
                PersistentDataType.INTEGER
            ) ?: return@forEachIndexed

            newLayout[index] = slot
        }

        currentLayout.filter { it.key !in newLayout.keys }.forEach { (kitItemIndex, slot) -> newLayout[kitItemIndex] = slot }

        loadouts.removeIf { it.id == kitUUID && it.player == player.tumblingPlayer }
        loadouts.add(KitLoadout(player.tumblingPlayer, kitUUID, newLayout))
    }

    fun giveKits(players: Set<Player>, kit: KitDefinition) {
        players.forEach {
            giveKit(it, kit)
        }
    }

    // loadout: key = kit item index, value = slot
    data class KitLoadout(val player: TumblingPlayer, val id: UUID, val loadout: HashMap<Int, Int>)
}