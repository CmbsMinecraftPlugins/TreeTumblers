package xyz.devcmb.tumblers.packet

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.base.AbstractGame
import xyz.devcmb.tumblers.util.tumblingPlayer

class GlowingPlayersListener : PacketListener {
    // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Entity_Metadata_Format
    // https://minecraft.wiki/w/Java_Edition_protocol/Packets#Set_Entity_Metadata
    override fun onPacketSend(event: PacketSendEvent) {
        if (
            event.packetType != PacketType.Play.Server.ENTITY_METADATA
            || GameController.activeGame == null
            || GameController.activeGame!!.currentState != AbstractGame.State.GAME_ON
            || GameController.activeGame!!.data.flags.contains(Flag.DISABLE_TEAM_GLOW)
        ) return

        val receiver = event.getPlayer<Player>()
        val packet = WrapperPlayServerEntityMetadata(event)
        val metaPlayer = Bukkit.getOnlinePlayers().find { it.entityId == packet.entityId } ?: return

        if(receiver.tumblingPlayer.team == metaPlayer.tumblingPlayer.team && receiver != metaPlayer) {
            packet.entityMetadata.add(EntityData(0, EntityDataTypes.BYTE, 0x40))
            event.markForReEncode(true)
        }
    }
}