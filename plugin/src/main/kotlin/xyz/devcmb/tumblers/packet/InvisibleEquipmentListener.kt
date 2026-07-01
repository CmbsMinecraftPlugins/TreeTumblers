package xyz.devcmb.tumblers.packet

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import xyz.devcmb.tumblers.util.tumblingPlayer

class InvisibleEquipmentListener : PacketListener {
    override fun onPacketSend(event: PacketSendEvent) {
        if(event.packetType != PacketType.Play.Server.ENTITY_EQUIPMENT) return

        val receiver = event.getPlayer<Player>()
        val packet = WrapperPlayServerEntityEquipment(event)
        val metaPlayer = Bukkit.getOnlinePlayers().find { it.entityId == packet.entityId } ?: return

        if(metaPlayer.hasPotionEffect(PotionEffectType.INVISIBILITY) && metaPlayer.tumblingPlayer.team != receiver.tumblingPlayer.team) {
            packet.equipment = packet.equipment.map {
                if(it.slot == EquipmentSlot.MAIN_HAND || it.slot == EquipmentSlot.OFF_HAND) it
                else Equipment(it.slot, SpigotConversionUtil.fromBukkitItemStack(ItemStack.of(Material.AIR)))
            }
            event.markForReEncode(true)
        }
    }
}