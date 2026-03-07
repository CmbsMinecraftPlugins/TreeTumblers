package xyz.devcmb.tumblers.engine.map.spawn

import xyz.devcmb.tumblers.data.Team

/**
 * A group of spawns for a certain game event
 * If the [spawnType] is [SpawnType.FIXED] then the [configList] should be a single list
 *
 * ```yaml
 * spawns:
 *    pregame:
 *      - [-32.5,79.0,-33.5]
 * ```
 * If the [spawnType] is [SpawnType.MATCHUP], then the [configList] should be a 2d list with spawns for both teams in the matchup
 * ```yaml
 *             ingame:
 *               arena1:
 *                 # The first set of spawns for the first team on this map
 *                 - [
 *                   [ -38.5,79.0,-43.5 ]
 *                   [ -38.5,79.0,-41.5 ]
 *                   [ -40.5,79.0,-39.5 ]
 *                   [ -42.5,79.0,-39.5 ]
 *                 ]
 *                 # The second set of spawns for the second team on this map
 *                 - [
 *                   [ -32.5,79.0,-29.5 ],
 *                   [ -32.5,79.0,-31.5 ],
 *                   [ -30.5,79.0,-33.5 ],
 *                   [ -28.5,79.0,-33.5 ]
 *                 ]
 * ```
 */
data class SpawnGroup(val configList: String, val spawnType: SpawnType, val team: Team? = null) {
    enum class SpawnType {
        FIXED,
        MATCHUP
    }
}