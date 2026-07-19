package xyz.devcmb.tumblers.controllers.games.tower_ascent.feature

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Ageable
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.player.SpectatorController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.equipArmor
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.isEnclosed
import xyz.devcmb.tumblers.util.isInRegion
import xyz.devcmb.tumblers.util.randomBetween
import xyz.devcmb.tumblers.util.runTaskLater
import xyz.devcmb.tumblers.util.subtitleCountdown
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.ticks
import xyz.devcmb.tumblers.util.toCenterXZLocation
import xyz.devcmb.tumblers.util.tp
import xyz.devcmb.tumblers.util.tumblingPlayer

class TowerHandler(
    private val map: LoadedMap,
    private val loadouts: ArrayList<TowerGenerator.MobLoadout>,
    private val spawnGroups: ArrayList<TowerGenerator.SpawnGroup>,
    private val rooms: ArrayList<TowerGenerator.LoadedRoom>
) : Listener {
    lateinit var team: Team
    var currentRoomIndex = 0
    val currentRoom: TowerGenerator.LoadedRoom
        get() = rooms[currentRoomIndex]

    var gameOn = false

    fun startGame() {
        gameOn = true
        TreeTumblers.pluginScope.launch {
            startRoom(rooms.first())
        }
    }

    suspend fun startRoom(loadedRoom: TowerGenerator.LoadedRoom) {
        Audience.audience(team.getOnlinePlayers()).showTitle(Title.title(
            Format.mm("<yellow>Room ${currentRoomIndex + 1}</yellow>"),
            Component.empty(),
            Title.Times.times(10.ticks, 50.ticks, 5.ticks)
        ))

        delay(50.ticks)
        subtitleCountdown(
            Audience.audience(team.getOnlinePlayers()),
            Format.mm("<yellow>Room ${currentRoomIndex + 1}</yellow>"),
            5
        )

        spawnMobs(loadedRoom, loadedRoom.room.mobSets)
    }

    val remainingSetMobs: ArrayList<Mob> = ArrayList()
    var currentMobSet = -1

    val elevatorBlocks: ArrayList<Location> = ArrayList()
    var elevatorOpen: Boolean = false
    suspend fun spawnMobs(room: TowerGenerator.LoadedRoom, sets: List<List<TowerGenerator.MobSet>>) {
        while(true) {
            if(remainingSetMobs.isNotEmpty()) {
                delay(300)
                continue
            }

            currentMobSet++

            if((currentMobSet + 1) > sets.size) {
                currentMobSet = -1
                remainingSetMobs.clear()

                Audience.audience(team.getOnlinePlayers()).showTitle(Title.title(
                    Format.mm("<green>Room Cleared!</green>"),
                    Component.empty(),
                    Title.Times.times(10.ticks, 40.ticks, 10.ticks)
                ))

                suspendSync {
                    room.endingElevatorBounds.first.forEachRegion(room.endingElevatorBounds.second) {
                        if(it.type == Material.IRON_BLOCK) {
                            elevatorBlocks.add(it.location)
                            it.type = Material.AIR
                        }
                    }
                    elevatorOpen = true
                }

                break
            }

            val mobSets = sets[currentMobSet]
            spawnMobSets(room, mobSets)
        }
    }

    suspend fun spawnMobSets(room: TowerGenerator.LoadedRoom, sets: List<TowerGenerator.MobSet>) {
        sets.forEach {
            val setGroup = spawnGroups.first { entry -> entry.id == it.group }
            val loadout = loadouts.first { entry -> entry.id == setGroup.loadout }

            suspendSync {
                repeat(it.amount) {
                    val location = room.roomBounds.first.randomBetween(room.roomBounds.second) { block ->
                        block.type != Material.AIR
                        && block.location.clone().add(0.0,1.0,0.0).block.type == Material.AIR
                        && block.location.clone().add(0.0, 2.0, 0.0).block.type == Material.AIR
                        && block.location.isEnclosed()
                    }!!.add(0.0,1.0,0.0).toCenterXZLocation()

                    map.world.spawn(location, setGroup.mob.entity) { mob ->
                        (mob as? Ageable)?.setAdult()
                        remainingSetMobs.add(mob)

                        mob.equipArmor(loadout.armor.map { armorItem -> armorItem.item.clone() })
                        mob.equipment.setItemInMainHand(loadout.weapon.item.clone())
                    }
                }
            }
        }
    }

    fun advanceRoom() {
        currentRoomIndex++
        team.getOnlinePlayers().forEach {
            val newLoc = currentRoom.startingElevatorBounds!!.first.randomBetween(currentRoom.startingElevatorBounds!!.second) { block ->
                block.type != Material.AIR
                && block.location.isEnclosed()
                && block.location.clone().add(0.0,1.0,0.0).block.type == Material.AIR
                && block.location.clone().add(0.0, 2.0, 0.0).block.type == Material.AIR
                && !block.location.isInRegion(currentRoom.roomBounds.first, currentRoom.roomBounds.second)
            }!!.add(0.0,1.0,0.0).toCenterXZLocation()
            it.tp(newLoc)
        }

        runTaskLater(25) {
            currentRoom.startingElevatorBounds!!.first.forEachRegion(currentRoom.startingElevatorBounds!!.second) {
                if(it.type == Material.IRON_BLOCK) it.type = Material.AIR
            }

            TreeTumblers.pluginScope.launch {
                delay(300)
                startRoom(currentRoom)
            }
        }
    }

    @EventHandler
    fun entityDeathEvent(event: EntityDeathEvent) {
        if(event.entity in remainingSetMobs) {
            remainingSetMobs.remove(event.entity)
            event.drops.clear()
        }
    }

    @EventHandler
    fun playerEndingElevatorEvent(event: PlayerMoveEvent) {
        val player = event.player
        val team = player.tumblingPlayer.team
        if(!gameOn || !team.playingTeam || SpectatorController.spectators.contains(player) || !elevatorOpen) return

        val bounds = currentRoom.endingElevatorBounds
        val checkBounds = bounds.first.clone().add(2.0,0.0,2.0) to bounds.second.clone().add(-2.0,0.0,-2.0)
        val predicate = { it: Player ->
            it.location.isInRegion(checkBounds.first, checkBounds.second)
            && !it.location.isInRegion(
                currentRoom.roomBounds.first.clone(),
                currentRoom.roomBounds.second.clone()
            )
            && it.location.toBlockLocation() !in elevatorBlocks
        }

        if(predicate(player) && team.getOnlinePlayers().filter { it != player }.all(predicate)) {
            elevatorOpen = false
            elevatorBlocks.forEach {
                it.block.type = Material.IRON_BLOCK
            }
            elevatorBlocks.clear()

            runTaskLater(25) {
                advanceRoom()
            }
        }
    }
}