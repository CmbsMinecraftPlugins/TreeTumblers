package xyz.devcmb.tumblers.controllers.games.party

import xyz.devcmb.tumblers.util.Kit

interface IndividualPartyGame {
    val id: String
    val kit: Kit.KitDefinition

    fun postSpawn()
    suspend fun start()
    fun cleanup()
}