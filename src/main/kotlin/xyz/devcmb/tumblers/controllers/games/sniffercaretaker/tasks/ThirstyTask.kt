package xyz.devcmb.tumblers.controllers.games.sniffercaretaker.tasks

import org.bukkit.Material
import org.bukkit.block.data.Levelled
import org.bukkit.entity.TextDisplay
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.SnifferCaretakerController
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.Task
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.runTaskLater
import xyz.devcmb.tumblers.util.tumblingPlayer

class ThirstyTask(
    override val team: Team,
    override val id: String,
    override val snifferCaretaker: SnifferCaretakerController,
    override val stars: Int,
    val item: Material?
) : Task {
    override val feeling = "thirsty"
    override var display: TextDisplay? = null

    override val displayText = Format.mm(
        "<font:${UserInterfaceUtility.ICONS}>${team.icon}</font> " +
                "<color:${team.color.asHexString()}>Sniffer</color> is ${feeling}! Bring " +
                "<sprite:items:item/${item?.name?.lowercase()}> <yellow><lang:${item?.itemTranslationKey}></yellow> to its cauldron!")

    @EventHandler
    fun playerInteract(event: PlayerInteractEvent) {
        val tumblingPlayer = event.player.tumblingPlayer
        if (tumblingPlayer.team != team) { return }

        val cauldron = event.clickedBlock
        if (cauldron == null) return
        if (cauldron.type != Material.CAULDRON) return

        val playerItem = event.item
        if (playerItem?.type != item) { return }

        if (event.hand == null) return

        event.isCancelled = true
        event.setUseItemInHand(Event.Result.DENY)
        event.setUseInteractedBlock(Event.Result.DENY)

        // the item in this task will always be a bucket, so..

        event.player.inventory.setItem(event.hand!!, ItemStack(Material.BUCKET))

        when (item) {
            Material.WATER_BUCKET -> {
                cauldron.type = Material.WATER_CAULDRON
            }
            Material.MILK_BUCKET -> {
                cauldron.type = Material.POWDER_SNOW_CAULDRON
            }
            else -> {}
        }

        (cauldron.blockData as Levelled).also {
            it.level = 3
        }

        snifferCaretaker.completeTask(this.team, this)

        runTaskLater(20*3) {
            cauldron.type = Material.CAULDRON
        }

    }
}