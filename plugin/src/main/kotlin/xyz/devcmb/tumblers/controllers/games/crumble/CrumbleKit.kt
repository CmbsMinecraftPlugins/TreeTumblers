package xyz.devcmb.tumblers.controllers.games.crumble

import org.bukkit.entity.Player
import org.bukkit.event.Listener
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.item.Kit

interface CrumbleKit : Listener {
    val player: TumblingPlayer
    val crumble: CrumbleController
    val companion: Companion

    fun onKill(killed: Player)
    fun onAbility()
    fun cleanup() {}

    interface Companion {
        val id: String
        val name: String

        val abilityName: String
        val abilityDescription: String

        val killPowerName: String
        val killPowerDescription: String

        val kit: Kit.KitDefinition

        fun create(player: TumblingPlayer, crumble: CrumbleController): CrumbleKit
    }
}