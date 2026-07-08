package xyz.devcmb.tumblers.item.custom

import xyz.devcmb.tumblers.item.advanced.AdvancedItemStack

interface CustomItem {
    fun build(): AdvancedItemStack
}