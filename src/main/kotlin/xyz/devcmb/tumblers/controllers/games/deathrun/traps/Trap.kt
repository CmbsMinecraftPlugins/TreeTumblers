package xyz.devcmb.tumblers.controllers.games.deathrun.traps

interface Trap {
    val id: String
    fun activate()
}