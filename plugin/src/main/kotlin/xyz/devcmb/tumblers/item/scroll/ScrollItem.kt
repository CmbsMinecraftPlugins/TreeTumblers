package xyz.devcmb.tumblers.item.scroll

import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.item.CustomItem
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.intToRoman
import xyz.devcmb.tumblers.util.item.AdvancedItemStack
import xyz.devcmb.tumblers.util.tickSeconds

class ScrollItem(
    val scrollEffect: ScrollEffect
): CustomItem {
    constructor() : this(ScrollEffect.JUMP_BOOST)

    companion object {
        val scrollKey = NamespacedKey(TreeTumblers.NAMESPACE, "scroll_item")
        val scrollCooldown: Float = configurable("item.scroll.cooldown")
    }

    override fun build(): AdvancedItemStack {
        return AdvancedItemStack(Material.ECHO_SHARD) {
            name(Format.mm("<yellow>Scroll of ${scrollEffect.scrollName}</yellow>"))
            model(NamespacedKey(TreeTumblers.NAMESPACE, "icon/scroll/${scrollEffect.name.lowercase()}"))

            useCooldown(scrollCooldown, scrollKey)
            lore(listOf(
                Format.mm("<gray>Gives you the <white>${scrollEffect.scrollName}" +
                    (if(scrollEffect.amplifier != 0) " ${intToRoman(scrollEffect.amplifier + 1)}" else "") +
                "</white></gray>"),
                Format.mm("<gray>effect for <white>${scrollEffect.duration.tickSeconds}s</white></gray>")
            ).map { it.decoration(TextDecoration.ITALIC, false) })

            click {
                amount -= 1
                it.addPotionEffect(PotionEffect(
                    scrollEffect.effect,
                    scrollEffect.duration.toInt(),
                    scrollEffect.amplifier,
                    false,
                    true,
                    true
                ))
            }
        }
    }

    enum class ScrollEffect(
        val scrollName: String,
        val effect: PotionEffectType,
        val duration: Long,
        val amplifier: Int
    ) {
        JUMP_BOOST("Jump Boost", PotionEffectType.JUMP_BOOST, 5 * 20, 1),
        REGENERATION("Regeneration", PotionEffectType.REGENERATION, 3 * 20, 2),
        INVISIBILITY("Invisibility", PotionEffectType.INVISIBILITY, 70, 0),
        SPEED("Speed", PotionEffectType.SPEED, 5 * 20, 1),
    }
}