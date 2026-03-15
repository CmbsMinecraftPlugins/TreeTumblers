package xyz.devcmb.tumblers.engine

import org.bukkit.entity.Player

abstract class DebugToolkit {
    abstract fun killEvent(killer: Player?, killed: Player?)
    abstract fun deathEvent(killed: Player?)
}