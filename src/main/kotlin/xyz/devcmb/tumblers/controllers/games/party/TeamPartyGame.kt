package xyz.devcmb.tumblers.controllers.games.party

interface TeamPartyGame {
    val id: String
    fun spawn()
    fun start()
    fun cleanup()
}