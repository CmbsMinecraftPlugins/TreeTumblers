package xyz.devcmb.tumblers.item.scroll

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.item.CustomItem
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.item.AdvancedItemStack

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

            click {
                amount -= 1
                it.addPotionEffect(PotionEffect(
                    scrollEffect.effect,
                    scrollEffect.duration,
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
        val duration: Int,
        val amplifier: Int
    ) {
        JUMP_BOOST("Jump Boost", PotionEffectType.JUMP_BOOST, 5 * 20, 1),
        REGENERATION("Regeneration", PotionEffectType.REGENERATION, 3 * 20, 1),
        INVISIBILITY("Invisibility", PotionEffectType.INVISIBILITY, 5 * 20, 0),
        SPEED("Speed", PotionEffectType.SPEED, 5 * 20, 1),
    }
}