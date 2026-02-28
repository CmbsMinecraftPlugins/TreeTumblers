package xyz.devcmb.tumblers

import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

class TreeTumblers : JavaPlugin() {
    companion object {
        lateinit var plugin: JavaPlugin
        lateinit var pluginLogger: Logger
    }

    override fun onEnable() {
        plugin = this
        pluginLogger = logger

        ControllerDelegate.registerAllControllers()
    }

    override fun onDisable() {
        ControllerDelegate.cleanupControllers()
    }
}
