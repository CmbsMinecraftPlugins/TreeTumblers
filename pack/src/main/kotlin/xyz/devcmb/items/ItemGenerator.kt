package xyz.devcmb.items

import xyz.devcmb.pack.ResourcePackBuilder

interface ItemGenerator {
    fun generateItems(builder: ResourcePackBuilder): List<GeneratedItem>
}