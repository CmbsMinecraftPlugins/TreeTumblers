package xyz.devcmb.tumblers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.bukkit.plugin.java.JavaPlugin
import xyz.devcmb.invcontrol.InvControlManager
import java.util.logging.Logger

class TreeTumblers : JavaPlugin() {
    companion object {
        lateinit var plugin: JavaPlugin
        lateinit var pluginLogger: Logger
        val pluginScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    override fun onEnable() {
        plugin = this
        pluginLogger = logger

        InvControlManager.setPlugin(this)
        ControllerDelegate.registerAllControllers()
    }

    override fun onDisable() {
        ControllerDelegate.cleanupControllers()
        pluginScope.cancel()
    }
}
