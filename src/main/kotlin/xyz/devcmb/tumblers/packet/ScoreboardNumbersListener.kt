package xyz.devcmb.tumblers.packet

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.score.ScoreFormat
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective

class ScoreboardNumbersListener : PacketListener {
    // https://github.com/maximjsx/CustomScoreNumbers/blob/master/core/src/main/java/com/maximde/customscores/core/packet/listeners/PacketSendListener.java
    override fun onPacketSend(event: PacketSendEvent?) {
        if (event!!.packetType !== PacketType.Play.Server.SCOREBOARD_OBJECTIVE) return

        val objective = WrapperPlayServerScoreboardObjective(event)
        objective.scoreFormat = ScoreFormat.blankScore()

        event.markForReEncode(true)
    }
}