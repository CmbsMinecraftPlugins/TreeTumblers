package xyz.devcmb.tumblers.ui

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.ui.bossbar.CountdownBossbar
import xyz.devcmb.tumblers.ui.bossbar.HandledBossbar
import xyz.devcmb.tumblers.ui.bossbar.games.crumble.AliveTeamsBossbar
import xyz.devcmb.tumblers.ui.inventory.HandledInventory
import xyz.devcmb.tumblers.ui.inventory.crumble.CrumbleKitSelector
import xyz.devcmb.tumblers.util.runTaskTimer

class PlayerUIController(val player: Player) {
    val inventories: ArrayList<HandledInventory> = ArrayList()
    val bossBars: ArrayList<HandledBossbar> = ArrayList()
    val activeBossBars: HashMap<String, BossBar> = HashMap()
    val paddingBossBars: HashMap<String, ArrayList<BossBar>> = HashMap()
    val gameController = ControllerDelegate.getController("gameController") as GameController

    init {
        registerInventory(CrumbleKitSelector(player, gameController))

        registerBossBar(AliveTeamsBossbar(gameController))
        registerBossBar(CountdownBossbar(gameController))

        runTaskTimer(0, 5) {
            bossBars.forEach {
                if(activeBossBars.containsKey(it.id)) {
                    val bar = activeBossBars[it.id]!!
                    bar.name(it.getComponent())
                }
            }
        }
    }

    fun registerInventory(inv: HandledInventory) {
        inventories.add(inv)
    }

    fun registerBossBar(bar: HandledBossbar) {
        bossBars.add(bar)
    }

    fun openInventory(id: String) {
        val handledInventory = inventories.find { it.id == id }
        if(handledInventory == null) throw IllegalArgumentException("Inventory with an id of $id does not exist")

        handledInventory.inventory.show()
    }

    fun enableBossBar(id: String) {
        val bar = bossBars.find { it.id == id }
        require(bar != null)

        if(activeBossBars.containsKey(id)) return

        val bossbar = BossBar.bossBar(
            bar.getComponent(),
            0f,
            BossBar.Color.WHITE,
            BossBar.Overlay.PROGRESS
        )

        player.showBossBar(bossbar)

        activeBossBars.put(id, bossbar)
        paddingBossBars.put(id, ArrayList())

        repeat(bar.padding) {
            val paddingBar = BossBar.bossBar(
                Component.empty(),
                0f,
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS
            )
            player.showBossBar(paddingBar)
            paddingBossBars[id]!!.add(paddingBar)
        }
    }

    fun disableBossBar(id: String) {
        val bar = bossBars.find { it.id == id }
        require(bar != null)

        if(!activeBossBars.containsKey(id)) return

        val activeBar = activeBossBars[id]!!
        activeBar.viewers().forEach {
            activeBar.removeViewer(it as Audience)
        }

        val paddingBars = paddingBossBars[id]!!
        paddingBars.forEach { bar ->
            bar.viewers().forEach {
                bar.removeViewer(it as Audience)
            }
        }

        activeBossBars.remove(id)
        paddingBossBars.remove(id)
    }
}