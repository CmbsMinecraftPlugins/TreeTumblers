package xyz.devcmb.tumblers.controllers

import org.bukkit.event.Listener
import xyz.devcmb.tumblers.ControllerRegistry

/**
 * Abstract class for controllers. Each controller controls a set of features.
 */
abstract class ControllerBase : Listener {
    /**
     * Gets called when the server has started the plugin during the onLoad cycle
     */
    abstract fun init()

    /**
     * Gets called when the server has finished its standard loading cycle. Triggered by a [org.bukkit.event.server.ServerLoadEvent]
     */
    open fun serverLoad() {}

    /**
     * Gets called when/if the server gracefully shuts down. This method has no garantee to be ran if the server crashes or is force ended
     */
    open fun cleanup() {}

    /**
     * Lazily gets a controller to prevent race conditions with loading
     */
    inline fun <reified T> controller(): Lazy<T> = ControllerRegistry.controller<T>()
}