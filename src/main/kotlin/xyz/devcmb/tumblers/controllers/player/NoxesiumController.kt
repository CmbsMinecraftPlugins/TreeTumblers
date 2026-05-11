package xyz.devcmb.tumblers.controllers.player

import com.noxcrew.noxesium.api.component.NoxesiumComponentType
import com.noxcrew.noxesium.api.feature.qib.QibDefinition
import com.noxcrew.noxesium.api.feature.qib.QibEffect
import com.noxcrew.noxesium.api.registry.NoxesiumRegistries
import com.noxcrew.noxesium.api.util.Unit
import com.noxcrew.noxesium.core.registry.CommonEntityComponentTypes
import com.noxcrew.noxesium.core.registry.CommonGameComponentTypes
import com.noxcrew.noxesium.paper.component.setNoxesiumComponent
import com.noxcrew.noxesium.paper.event.NoxesiumPlayerRegisteredEvent
import net.kyori.adventure.key.Key
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Interaction
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.ControllerBase

@Controller(Controller.Priority.MEDIUM)
class NoxesiumController : ControllerBase() {
    override fun init() {
        QibType.entries.forEach {
            it.register()
        }
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

    enum class QibType(val key: Key) {
        JUMP_PAD(Key.key("tumbling", "jump_pad")) {
            override fun register() {
                val effectDefinition = TreeTumblers.Companion.plugin.getResource("qibs/jump_pad.json")!!
                val data = effectDefinition.bufferedReader(Charsets.UTF_8).use { content -> content.readText() }
                val effect = QibDefinition.QIB_GSON.fromJson(data, QibEffect::class.java)

                NoxesiumRegistries.QIB_EFFECTS.register(key, QibDefinition(
                    null,
                    null,
                    null,
                    effect,
                    null,
                    null,
                    false
                )
                )
            }

            override fun spawn(location: Location) {
                location.block.type = Material.GREEN_GLAZED_TERRACOTTA
                location.world.spawn(location, Interaction::class.java) {
                    it.interactionHeight = 2.5f
                    it.interactionWidth = 2.5f
                    it.isInvisible = true
                    it.setNoxesiumComponent(CommonEntityComponentTypes.QIB_BEHAVIOR, key)
                }
            }
        },
        LAUNCH_PAD(Key.key("tumbling", "boost_pad")) {
            override fun register() {
                val effectDefinition = TreeTumblers.Companion.plugin.getResource("qibs/launch_pad.json")!!
                val data = effectDefinition.bufferedReader(Charsets.UTF_8).use { content -> content.readText() }
                val effect = QibDefinition.QIB_GSON.fromJson(data, QibEffect::class.java)

                NoxesiumRegistries.QIB_EFFECTS.register(key, QibDefinition(
                    null,
                    null,
                    null,
                    effect,
                    null,
                    null,
                    false
                )
                )
            }

            override fun spawn(location: Location) {
                location.block.type = Material.RED_GLAZED_TERRACOTTA
                location.world.spawn(location, Interaction::class.java) {
                    it.interactionHeight = 2.5f
                    it.interactionWidth = 2.5f
                    it.isInvisible = true
                    it.setNoxesiumComponent(CommonEntityComponentTypes.QIB_BEHAVIOR, key)
                }
            }
        },
        ULTRA_LAUNCH_PAD(Key.key("tumbling", "ultra_boost_pad")) {
            override fun register() {
                val effectDefinition = TreeTumblers.Companion.plugin.getResource("qibs/ultra_launch_pad.json")!!
                val data = effectDefinition.bufferedReader(Charsets.UTF_8).use { content -> content.readText() }
                val effect = QibDefinition.QIB_GSON.fromJson(data, QibEffect::class.java)

                NoxesiumRegistries.QIB_EFFECTS.register(key, QibDefinition(
                    null,
                    null,
                    null,
                    effect,
                    null,
                    null,
                    false
                )
                )
            }

            override fun spawn(location: Location) {
                location.block.type = Material.ORANGE_GLAZED_TERRACOTTA
                location.world.spawn(location, Interaction::class.java) {
                    it.interactionHeight = 2.5f
                    it.interactionWidth = 2.5f
                    it.isInvisible = true
                    it.setNoxesiumComponent(CommonEntityComponentTypes.QIB_BEHAVIOR, key)
                }
            }
        };

        abstract fun register()
        abstract fun spawn(location: Location)
    }
}