package xyz.devcmb.tumblers.packet

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import org.bukkit.Bukkit
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.formattedName
import java.util.Optional

class OverheadNameListener : PacketListener {
    override fun onPacketSend(event: PacketSendEvent) {
        if(event.packetType != PacketType.Play.Server.ENTITY_METADATA) return

        val packet = WrapperPlayServerEntityMetadata(event)
        val player = Bukkit.getOnlinePlayers().find { it.entityId == packet.entityId }

        if(player == null) {
            DebugUtil.severe("Attempted to update metadata for a nonexistent player ${packet.entityId}")
            return
        }

        packet.entityMetadata.add(EntityData(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(player.formattedName)))
        event.markForReEncode(true)
    }
}