package xyz.devcmb.tumblers.item

import xyz.devcmb.tumblers.util.item.AdvancedItemStack

interface CustomItem {
    fun build(): AdvancedItemStack
}