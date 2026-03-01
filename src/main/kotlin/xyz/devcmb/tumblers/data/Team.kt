package xyz.devcmb.tumblers.data

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

enum class Team(val teamName: String, val color: TextColor, val icon: String, val playingTeam: Boolean = true) {
    RED("Red Rabbits", NamedTextColor.RED, "\uE000"),
    ORANGE("Orange Ocelots", TextColor.fromHexString("#ff9100")!!, "\uE001"),
    YELLOW("Yellow Yaks", NamedTextColor.YELLOW, "\uE002"),
    GREEN("Green Grasshoppers", NamedTextColor.GREEN,  "\uE003"),
    BLUE("Blue Boars", NamedTextColor.BLUE, "\uE004"),
    PURPLE("Purple Parrots", TextColor.fromHexString("#bb00ff")!!, "\uE005"),

    // Non-playing teams
    SPECTATORS("Spectators", NamedTextColor.WHITE, "\uE007", false),
    DEVELOPERS("Developers", TextColor.fromHexString("#00c8ff")!!, "\uE008", false)
}