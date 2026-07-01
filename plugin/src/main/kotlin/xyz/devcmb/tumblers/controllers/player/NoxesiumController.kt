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
import xyz.devcmb.tumblers.TumblingNoxesiumException
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.IController

@Controller(Controller.Priority.MEDIUM)
object NoxesiumController : IController {
    override fun init() {
        QibType.entries.forEach {
            it.register()
        }
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
        JUMP_PAD(Key.key(TreeTumblers.NAMESPACE, "jump_pad")) {
            override fun register() {
                val enterEffect = getEffect("qibs/jump_pad_enter.json")
                val leaveEffect = getEffect("qibs/jump_pad_leave.json")

                NoxesiumRegistries.QIB_EFFECTS.register(key, QibDefinition(
                    enterEffect,
                    leaveEffect,
                    null,
                    null,
                    null,
                    null,
                    false
                ))
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
        LAUNCH_PAD(Key.key(TreeTumblers.NAMESPACE, "boost_pad")) {
            override fun register() {
                val effect = getEffect("qibs/launch_pad.json")

                NoxesiumRegistries.QIB_EFFECTS.register(key, QibDefinition(
                    null,
                    null,
                    null,
                    effect,
                    null,
                    null,
                    false
                ))
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
        ULTRA_LAUNCH_PAD(Key.key(TreeTumblers.NAMESPACE, "ultra_boost_pad")) {
            override fun register() {
                val effect = getEffect("qibs/ultra_launch_pad.json")

                NoxesiumRegistries.QIB_EFFECTS.register(key, QibDefinition(
                    null,
                    null,
                    null,
                    effect,
                    null,
                    null,
                    false
                ))
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

        fun getEffect(file: String): QibEffect {
            val effectDefinition = TreeTumblers.plugin.getResource(file)
                ?: throw TumblingNoxesiumException("Attempted to load a noxesium effect for a nonexistent file $file")

            val data = effectDefinition.bufferedReader(Charsets.UTF_8).use { content -> content.readText() }
            return QibDefinition.QIB_GSON.fromJson(data, QibEffect::class.java)
        }
    }
}