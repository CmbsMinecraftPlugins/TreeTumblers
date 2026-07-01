package xyz.devcmb.tumblers.ui.bossbar

import net.kyori.adventure.text.Component

interface HandledBossbar {
    val id: String
    val padding: Int
    fun getComponent(): Component
}