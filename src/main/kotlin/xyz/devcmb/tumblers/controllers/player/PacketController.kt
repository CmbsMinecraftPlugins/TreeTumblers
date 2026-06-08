package xyz.devcmb.tumblers.controllers.player

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketListenerPriority
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.ControllerBase
import xyz.devcmb.tumblers.packet.GlowingPlayersListener
import xyz.devcmb.tumblers.packet.InvisibleEquipmentListener
import xyz.devcmb.tumblers.packet.ScoreboardNumbersListener

@Controller(Controller.Priority.HIGH)
class PacketController : ControllerBase() {
    override fun init() {
        val api = SpigotPacketEventsBuilder.build(TreeTumblers.plugin)
        PacketEvents.setAPI(api)
        PacketEvents.getAPI().load()

        loadListener(ScoreboardNumbersListener())
        loadListener(GlowingPlayersListener())
        loadListener(InvisibleEquipmentListener())
    }

    private fun loadListener(listener: PacketListener) {
        PacketEvents.getAPI().eventManager.registerListener(listener, PacketListenerPriority.LOW)
    }

    override fun cleanup() {
        PacketEvents.getAPI().terminate()
    }

    override fun serverLoad() {
        PacketEvents.getAPI().init()
    }
}
