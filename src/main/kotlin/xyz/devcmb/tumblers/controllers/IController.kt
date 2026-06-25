package xyz.devcmb.tumblers.controllers

import org.bukkit.event.Listener

/**
 * Abstract class for controllers. Each controller controls a set of features.
 */
interface IController : Listener {
    /**
     * Gets called when the server has started the plugin during the onLoad cycle
     */
    fun init()

    /**
     * Gets called when the server has finished its standard loading cycle. Triggered by a [org.bukkit.event.server.ServerLoadEvent]
     */
    fun serverLoad() {}

    /**
     * Gets called when/if the server gracefully shuts down. This method has no garantee to be ran if the server crashes or is force ended
     */
    fun cleanup() {}
}