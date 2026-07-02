package xyz.devcmb.models

import xyz.devcmb.pack.ResourcePackBuilder

interface ModelGenerator {
    fun generateModels(builder: ResourcePackBuilder): List<GeneratedModel>
}