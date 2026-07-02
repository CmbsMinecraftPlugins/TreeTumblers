package xyz.devcmb.models

import xyz.devcmb.util.IdentifiedResource

/**
 * The resource is relative to assets/namespace/models
 */
data class GeneratedModel(
    val resource: IdentifiedResource,
    val contents: String
)