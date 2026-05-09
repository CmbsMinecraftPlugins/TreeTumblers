package xyz.devcmb.tumblers.controllers

import com.noxcrew.noxesium.api.component.NoxesiumComponentType
import com.noxcrew.noxesium.api.util.Unit
import com.noxcrew.noxesium.core.registry.CommonGameComponentTypes
import com.noxcrew.noxesium.paper.event.NoxesiumPlayerRegisteredEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import xyz.devcmb.tumblers.annotations.Controller

@Controller("noxesiumController")
class NoxesiumController : IController {
    override fun init() {
    }

    override fun cleanup() {
    }

    val gameComponentRules: HashMap<NoxesiumComponentType<*>, Any> = hashMapOf(
        CommonGameComponentTypes.DISABLE_VANILLA_MUSIC to Unit.INSTANCE
    )

    @Suppress("UNCHECKED_CAST")
    @EventHandler(priority = EventPriority.HIGHEST)
    fun playerJoin(event: NoxesiumPlayerRegisteredEvent) {
        gameComponentRules.forEach {
            event.noxesiumPlayer.gameComponents.`noxesium$setComponent`(it.key as NoxesiumComponentType<Any>, it.value)
        }
    }
}