package xyz.devcmb.tumblers

import org.bukkit.Bukkit
import org.bukkit.plugin.PluginManager
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.IController
import xyz.devcmb.tumblers.util.DebugUtil

object ControllerDelegate {
    val controllers: HashMap<String, IController> = HashMap()

    @Suppress("UNCHECKED_CAST")
    fun registerAllControllers() {
        val reflections = Reflections(
            TreeTumblers::class.java.packageName,
            Scanners.TypesAnnotated
        )

        reflections.getTypesAnnotatedWith(Controller::class.java)
            .filter { IController::class.java.isAssignableFrom(it) }
            .sortedByDescending { it.getAnnotation(Controller::class.java).priority.value }
            .forEach { clazz ->
                val annotation = clazz.getAnnotation(Controller::class.java)
                val controllerClass = clazz as Class<out IController>

                val instance: IController =
                    controllerClass.getDeclaredConstructor().newInstance()

                registerController(annotation.id, instance)
            }
    }


    fun registerController(id: String, controller: IController) {
        val manager: PluginManager = Bukkit.getServer().pluginManager
        manager.registerEvents(controller, TreeTumblers.plugin)

        controllers[id] = controller
        controller.init() // guess who forgot this :eyes:
        DebugUtil.success("Controller $id registered successfully")
    }

    fun getController(id: String): IController? {
        val controller: IController? = controllers[id]
        if(controller == null) {
            DebugUtil.severe("Controller with id $id not found")
            return null
        }

        return controller
    }

    inline fun <reified T> getController(): T? {
        val controller: T? = controllers.values.find { it is T } as T?
        if(controller == null) {
            DebugUtil.severe("Controller with class ${T::class.java.name} not found")
            return null
        }

        return controller
    }

    fun cleanupControllers() {
        controllers.forEach {
            it.value.cleanup()
        }
    }
}