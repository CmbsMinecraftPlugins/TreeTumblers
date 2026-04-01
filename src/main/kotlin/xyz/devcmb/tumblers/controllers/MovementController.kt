package xyz.devcmb.tumblers.controllers

import com.noxcrew.noxesium.api.protocol.rule.ServerRuleIndices
import com.noxcrew.noxesium.api.qib.QibDefinition
import com.noxcrew.noxesium.api.qib.QibEffect
import com.noxcrew.noxesium.paper.api.NoxesiumManager
import com.noxcrew.noxesium.paper.api.rule.EntityRules
import com.noxcrew.noxesium.paper.api.rule.ServerRules
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.slf4j.LoggerFactory
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.MiscUtils

/*
 * Huge props to NoxesiumUtils for being open source :pray:
 * https://github.com/SuperNeon4ik/NoxesiumUtils
 */

@Controller("movementController", Controller.Priority.HIGH)
class MovementController : IController {
    lateinit var manager: NoxesiumManager
    val qibEffects: HashMap<String, QibEffect> = HashMap()
    val qibDefinitions: HashMap<String, QibDefinition> = HashMap()

    lateinit var serverRules: ServerRules
    lateinit var entityRules: EntityRules

    override fun init() {
        manager = NoxesiumManager(TreeTumblers.plugin, LoggerFactory.getLogger("TreeTumblersNoxesium"))

        serverRules = ServerRules(manager)
        entityRules = EntityRules(manager)

        loadQibEffects()
        loadQibDefinitions()
    }

    @EventHandler
    fun playerJoinEvent(event: PlayerJoinEvent) {
        manager.getServerRule<Any>(event.player, ServerRuleIndices.QIB_BEHAVIORS)
            ?.value = qibDefinitions
    }

    fun loadQibEffects() {
        val qibFolder = MiscUtils.listResourceFolder("qibs")

        val qibGson = QibDefinition.QIB_GSON
        qibFolder.forEach {
            val path = it.path.replace("\\", "/")

            val stream = TreeTumblers.plugin.getResource(path)!!
            val data = stream.bufferedReader(Charsets.UTF_8).use { content -> content.readText() }

            val output = qibGson.fromJson(data, QibEffect::class.java)
            val name = it.name.replace(".json", "")
            DebugUtil.info("Loaded ${it.name} QIB effect!")
            qibEffects.put(name, output)
        }
    }

    fun loadQibDefinitions() {
        loadQibDefinition("jump_pad", null, null, null, qibEffects["jump_pad"]!!, false)
    }

    fun loadQibDefinition(id: String, onEnter: QibEffect?, onLeave: QibEffect?, whileInside: QibEffect?, onJump: QibEffect?, triggerEnterLeaveOnSwitch: Boolean) {
        qibDefinitions.put(id, QibDefinition(onEnter, onLeave, whileInside, onJump, triggerEnterLeaveOnSwitch))
    }
}