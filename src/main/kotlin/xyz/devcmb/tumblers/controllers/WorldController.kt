package xyz.devcmb.tumblers.controllers

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.WorldCreator
import xyz.devcmb.tumblers.WorldCreationError
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.util.MiscUtils
import java.io.File

@Controller("worldController", Controller.Priority.MEDIUM)
class WorldController : IController {
    override fun init() {
    }

    fun createVoidWorld(worldName: String): World {
        val world = Bukkit.createWorld(WorldCreator(worldName)
            .generator(MiscUtils.VoidGenerator))!!

        world.getBlockAt(0, 64, 0).type = Material.STONE

        val file = File(Bukkit.getWorldContainer(), worldName)
        if(!file.exists() || !file.isDirectory) {
            Bukkit.unloadWorld(worldName, false)
            throw WorldCreationError("World does not have a world folder in the bukkit world container")
        }



        return world
    }
}