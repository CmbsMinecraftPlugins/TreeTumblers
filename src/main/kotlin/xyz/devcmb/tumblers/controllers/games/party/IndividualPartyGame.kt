package xyz.devcmb.tumblers.controllers.games.party

interface IndividualPartyGame {
    val id: String
    fun spawn()
    fun start()
    fun cleanup()
}