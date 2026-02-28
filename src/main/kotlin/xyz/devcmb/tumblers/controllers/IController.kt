package xyz.devcmb.tumblers.controllers

import org.bukkit.event.Listener

interface IController : Listener {
    fun init()
    fun cleanup() {}
}