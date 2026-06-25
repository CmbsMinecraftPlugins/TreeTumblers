package xyz.devcmb.tumblers.controllers.player

import io.papermc.paper.util.Tick
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.title.Title
import org.bukkit.entity.Arrow
import org.bukkit.entity.Firework
import org.bukkit.entity.TNTPrimed
import org.bukkit.entity.Trident
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.IController
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.runTaskTimer

@Controller(Controller.Priority.LOW)
object PlayerEventController : IController {
    val queuedEvents: HashMap<TumblingPlayer, ArrayList<Event>> = HashMap()

    override fun init() {
        runTaskTimer(0, 5) {
            queuedEvents.forEach { (_, events) ->
                if(events.isEmpty()) return@forEach

                val nextEvent = events[0]
                nextEvent.player.bukkitPlayer?.showTitle(Title.title(
                    nextEvent.title.first,
                    nextEvent.title.second,
                    Title.Times.times(Tick.of(0), Tick.of(30), Tick.of(3))
                ))

                events.removeFirst()
            }
        }
    }

    fun addEvent(player: TumblingPlayer, event: Event) {
        queuedEvents.putIfAbsent(player, arrayListOf())
        queuedEvents[player]!!.add(event)
    }

    interface Event {
        val player: TumblingPlayer
        val title: Pair<Component, Component>

        class KillEvent(
            override val player: TumblingPlayer,
            val killed: TumblingPlayer,
            val score: Int?
        ) : Event {
            override val title: Pair<Component, Component>
                get() {
                    val source: EntityDamageEvent? = killed.bukkitPlayer?.lastDamageCause
                    var icon = "\uD83D\uDDE1"

                    if(source is EntityDamageByEntityEvent) {
                        icon = when (source.damager) {
                            is Arrow,
                            is Firework -> "\uD83C\uDFF9"

                            is Trident -> "\uD83D\uDD31"
                            is TNTPrimed -> "\uD83D\uDCA3"
                            else -> "\uD83D\uDDE1"
                        }
                    }

                    return Component.empty() to Format.mm(
                        "<red>$icon</red> <player>${if(score != null) " <gold>[+${score}]</gold>" else ""}",
                        Placeholder.component("player", Format.formatPlayerName(killed))
                    )
                }
        }
    }
}