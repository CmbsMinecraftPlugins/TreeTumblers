package xyz.devcmb.tumblers.controllers.games.party

import net.kyori.adventure.text.Component
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.engine.GameBase

@EventGame
class PartyController : GameBase(
    id = "party",
    name = "Party",
    votable = true,
    maps = setOf(),
    cutsceneSteps = arrayListOf(),
    flags = setOf(),
    scores = hashMapOf(),
    icon = Component.empty(),
    scoreboard = "partyScoreboard"
) {
    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    override suspend fun gameLoad() {
        TODO("Not yet implemented")
    }

    /**
     * The abstract method for spawning players in
     *
     * There was going to be some kind of system to do this automatically, but doing it manually seems to be a more flexible option, at least for now.
     *
     * @param cycle The stage where the players are spawned
     */
    override suspend fun spawn(cycle: SpawnCycle) {
        TODO("Not yet implemented")
    }

    /**
     * The method for the main gameplay loop for an individual game
     *
     * This should contain any kind of game-specific logic, and round handling if applicable
     */
    override suspend fun gameOn() {
        TODO("Not yet implemented")
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {
        TODO("Not yet implemented")
    }
}