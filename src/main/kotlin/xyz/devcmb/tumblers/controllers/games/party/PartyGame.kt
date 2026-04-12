package xyz.devcmb.tumblers.controllers.games.party

import xyz.devcmb.tumblers.util.Kit

interface PartyGame {
    val id: String
    val kit: Kit.KitDefinition

    val team: Boolean
    val individual: Boolean

    fun postSpawn()
    suspend fun start()
    fun cleanup()
}