package xyz.devcmb.tumblers.controllers.games.sniffercaretaker.tasks

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.SnifferCaretakerController
import xyz.devcmb.tumblers.controllers.games.sniffercaretaker.Task
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.tumblingPlayer
import kotlin.random.Random

class HungryTask(
    override val team: Team,
    override val id: String,
    override val snifferCaretaker: SnifferCaretakerController,
    override val stars: Int,
    val item: Material?
) : Task {
    override val feeling = "hungry"
    override var display: TextDisplay? = null
    override var count = 1
    override var completer: Player? = null

    val atlas = if (item == Material.BROWN_MUSHROOM || item == Material.RED_MUSHROOM) "block" else "item"

    override var displayText = ""
        get() = "<font:${UserInterfaceUtility.ICONS}>${team.icon}</font> " +
                    "<color:${team.color.asHexString()}>Sniffer</color> is ${feeling}! Feed it " +
                    "<sprite:${atlas}s:${atlas}/${item?.name?.lowercase()}> <yellow><lang:${item?.itemTranslationKey}>!</yellow>"


    @EventHandler
    fun playerInteractEntity(event: PlayerInteractEntityEvent) {
        val tumblingPlayer = event.player.tumblingPlayer
        if (tumblingPlayer.team != team) return

        val sniffer = event.rightClicked
        if (sniffer.type != EntityType.SNIFFER) return
        if (sniffer.persistentDataContainer.get(SnifferCaretakerController.snifferTeamKey, PersistentDataType.STRING) != team.name) return

        val playerItem = event.player.inventory.getItem(event.hand)
        if (playerItem.type != item) return

        if (item == Material.MUSHROOM_STEW) {
            event.player.inventory.setItem(event.hand, ItemStack(Material.BOWL))
            // that's right, we're gonna Hard Code :steamhappy:
        } else {
            playerItem.amount -= 1
        }

        snifferCaretaker.currentMap.world.playSound(sniffer.location, Sound.ENTITY_SNIFFER_EAT, 1.0f, 0.8f + (Random.nextFloat() * 0.4f))
        snifferCaretaker.currentMap.world.spawnParticle(
            Particle.ITEM,
            sniffer.location.add(sniffer.location.direction.multiply(2)).add(0.0,0.5,0.0),
            60,
            0.1,0.1,0.1,
            0.15,
            ItemStack.of(item)
        )

        this.completer = event.player

        snifferCaretaker.completeTask(this.team, this)
    }
}