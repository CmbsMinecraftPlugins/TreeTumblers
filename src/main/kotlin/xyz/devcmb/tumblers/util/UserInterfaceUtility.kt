package xyz.devcmb.tumblers.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey

object UserInterfaceUtility {
    val SPACES = NamespacedKey("tumbling", "spaces")
    val WARNINGS = NamespacedKey("tumbling", "warnings")

    fun constructLine(length: Int, color: NamedTextColor = NamedTextColor.WHITE): Component {
        var component = Component.empty()

       repeat(length) {
            component = component.append(
                Component.text("—", color)
                    .append(
                        Component.text("\uF000")
                            .font(SPACES)
                    )
            )
        }

        return component
    }
}