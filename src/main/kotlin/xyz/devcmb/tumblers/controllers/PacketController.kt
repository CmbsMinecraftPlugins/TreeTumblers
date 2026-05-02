package xyz.devcmb.tumblers.controllers

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketListenerPriority
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.event.EventHandler
import org.bukkit.event.server.ServerLoadEvent
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.packet.GlowingPlayersListener
import xyz.devcmb.tumblers.packet.OverheadNameListener
import xyz.devcmb.tumblers.packet.ScoreboardNumbersListener

@Controller("packetController", Controller.Priority.MEDIUM)
class PacketController : IController {
    override fun init() {
        val api = SpigotPacketEventsBuilder.build(TreeTumblers.plugin)
        PacketEvents.setAPI(api)
        PacketEvents.getAPI().load()

        loadListener(ScoreboardNumbersListener(), PacketListenerPriority.LOW)
        loadListener(GlowingPlayersListener(), PacketListenerPriority.LOW)
        loadListener(OverheadNameListener(), PacketListenerPriority.LOW)
    }

    private fun loadListener(listener: PacketListener, priority: PacketListenerPriority) {
        PacketEvents.getAPI().eventManager.registerListener(listener, priority)
    }

    override fun cleanup() {
        PacketEvents.getAPI().terminate()
    }

    @EventHandler
    fun serverLoad(event: ServerLoadEvent) {
        PacketEvents.getAPI().init()
    }
}