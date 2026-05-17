package xyz.devcmb.tumblers.engine.cutscene

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.hideToAll
import xyz.devcmb.tumblers.util.showToAll

/**
 * A single step of a cutscene.
 *
 * These are for explaining the game before it begins
 *
 * @param chatMessage The message to send all observers of the cutscene
 * @param run The function to run with a [LoadedMap] param and an attached [CutsceneContext]
 */
class CutsceneStep(
    val chatMessage: Component?,
    val startingTeleport: String? = null,
    val run: suspend CutsceneContext.(map: LoadedMap) -> Unit
)