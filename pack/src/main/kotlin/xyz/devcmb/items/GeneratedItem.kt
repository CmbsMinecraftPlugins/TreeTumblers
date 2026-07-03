package xyz.devcmb.items

import xyz.devcmb.util.IdentifiedResource

/**
 * The resource is relative to assets/namespace/items
 */
data class GeneratedItem(
    val resource: IdentifiedResource,
    val contents: String
)