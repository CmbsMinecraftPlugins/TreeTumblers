package xyz.devcmb.tumblers.controllers.games.sniffercaretaker.tasks

import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.persistence.PersistentDataType
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.SnifferCaretakerController
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.Task
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer

class HungryTask(
    override val team: Team,
    override val id: String,
    override val snifferCaretaker: SnifferCaretakerController,
    override val stars: Int,
    val item: Material?
) : Task {
    override val feeling = "hungry"
    // todo: pretty this up, maybe, like color and stuff in the messages, this is just proof of concept ..?
    override val description = "Feed it "
    override var display: TextDisplay? = null

    val displayText = Format.mm(
        "<font:${UserInterfaceUtility.ICONS}>${team.icon}</font> " +
                "<color:${team.color.asHexString()}>Sniffer</color> is ${feeling}! $description" +
                "<sprite:items:item/${item?.name?.lowercase()}> <yellow><lang:${item?.itemTranslationKey}>!</yellow>")

    @EventHandler
    fun playerInteractEntity(event: PlayerInteractEntityEvent) {
        val tumblingPlayer = event.player.tumblingPlayer
        if (tumblingPlayer.team != team) { return }

        val sniffer = event.rightClicked
        if (sniffer.type != EntityType.SNIFFER) { return }
        if (sniffer.persistentDataContainer.get(SnifferCaretakerController.snifferTeamKey, PersistentDataType.STRING) != team.name) { return }

        val playerItem = event.player.inventory.getItem(event.hand)
        if (playerItem.type != item) { return }

        playerItem.amount -= 1

        taskComplete()
    }
}