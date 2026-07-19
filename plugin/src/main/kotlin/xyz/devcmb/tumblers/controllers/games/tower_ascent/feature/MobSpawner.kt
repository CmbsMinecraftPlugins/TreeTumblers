package xyz.devcmb.tumblers.controllers.games.tower_ascent.feature

import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.Husk
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Skeleton
import org.bukkit.entity.Stray
import org.bukkit.entity.Zombie
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.controllers.games.tower_ascent.TowerAscentController
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.validateElements
import xyz.devcmb.tumblers.util.validateList
import java.util.HashMap

class MobSpawner(private val controller: TowerAscentController, private val map: LoadedMap) {
    val loadouts: ArrayList<MobLoadout> = ArrayList()
    val spawnGroups: ArrayList<SpawnGroup> = ArrayList()

    fun loadMobSets() {
        val mapLoadouts = map.data.getList("loadouts")?.mapIndexed { index, loadout ->
            if(
                loadout !is HashMap<*, *>
                || !loadout.validateElements(hashMapOf(
                    "id" to { it is String },
                    "armor" to {
                        it is List<*>
                        && it.validateList<String>() != null
                        && it.all { entry ->
                            MobArmorItem.entries.any { armorEntry -> armorEntry.id.equals((entry as String), true) }
                        }
                    },
                    "weapon" to { it is String && MobWeaponItem.entries.any { entry -> entry.id.equals(it, true) } }
                ))
            ) throw GameControllerException("Loadout $index is not properly formatted")

            MobLoadout(
                loadout["id"] as String,
                (loadout["armor"] as List<*>).map {
                    MobArmorItem.entries.find { entry -> entry.id.equals(it as String, true) }!!
                },
                MobWeaponItem.entries.find { entry -> entry.id.equals(loadout["weapon"] as String, true) }!!
            )
        } ?: throw GameControllerException("Map loadouts for ${map.id} were not provided")

        val mapSpawnGroups = map.data.getList("spawn_groups")?.mapIndexed { index, group ->
            if(
                group !is HashMap<*, *>
                || !group.validateElements(hashMapOf(
                    "id" to { it is String },
                    "mob" to { it is String && SpawnableMob.entries.any { entry -> entry.id.equals(it, true)} },
                    "loadout" to { it is String && mapLoadouts.any { entry -> entry.id.equals(it, true) } }
                ))
            ) throw GameControllerException("Spawn group $index is not properly formatted")

            SpawnGroup(
                group["id"] as String,
                SpawnableMob.entries.find { it.id.equals(group["mob"] as String, true) }!!,
                group["loadout"] as String,
            )
        } ?: throw GameControllerException("Spawn groups for map ${map.id} were not provided")

        loadouts.addAll(mapLoadouts)
        spawnGroups.addAll(mapSpawnGroups)

        DebugUtil.info("Loaded the following loadouts: $mapLoadouts")
        DebugUtil.info("Loaded the following spawn groups: $mapSpawnGroups")
    }

    data class SpawnGroup(
        val id: String,
        val mob: SpawnableMob,
        /** Relational to a [MobLoadout.id] */
        val loadout: String
    )

    data class MobLoadout(
        val id: String,
        val armor: List<MobArmorItem>,
        val weapon: MobWeaponItem
    )

    enum class SpawnableMob(val id: String, val entity: Class<out Mob>) {
        SKELETON("skeleton", Skeleton::class.java),
        STRAY("stray", Stray::class.java),
        ZOMBIE("zombie", Zombie::class.java),
        HUSK("husk", Husk::class.java)
    }

    enum class MobWeaponItem(val id: String, val item: ItemStack) {
        BASE_BOW("base_bow", ItemStack(Material.BOW)),
        STONE_SWORD("stone_sword", ItemStack(Material.STONE_SWORD)),
        IRON_SWORD("iron_sword", ItemStack(Material.IRON_SWORD)),
    }

    enum class MobArmorItem(val id: String, val item: ItemStack) {
        BASE_IRON_CHESTPLATE("base_iron_chestplate", ItemStack(Material.IRON_CHESTPLATE)),
        BASE_GOLD_HELMET("base_gold_helmet", ItemStack(Material.GOLDEN_HELMET)),
        BASE_LEATHER_CHESTPLATE("base_leather_chestplate", ItemStack(Material.LEATHER_CHESTPLATE)),
        BASE_IRON_BOOTS("base_iron_boots", ItemStack(Material.IRON_BOOTS)),
    }
}