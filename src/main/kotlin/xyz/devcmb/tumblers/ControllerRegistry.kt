package xyz.devcmb.tumblers

import org.bukkit.Bukkit
import org.bukkit.plugin.PluginManager
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.IController
import xyz.devcmb.tumblers.util.DebugUtil

object ControllerRegistry {
    val controllers: MutableSet<IController> = HashSet()

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
                val controllerClass = clazz as Class<out IController>

                val instance: IController =
                    controllerClass.getDeclaredConstructor().newInstance()

                registerController(instance)
            }
    }


    fun registerController(controller: IController) {
        val manager: PluginManager = Bukkit.getServer().pluginManager
        manager.registerEvents(controller, TreeTumblers.plugin)

        controllers.add(controller)
        controller.init() // guess who forgot this :eyes:
        DebugUtil.success("Registered controller ${controller::class.java.simpleName} successfully")
    }

    inline fun <reified T> getController(): T {
        val controller: T? = controllers.find { it is T } as T?
        if(controller == null) throw IllegalArgumentException("Controller with the class ${T::class.java.simpleName} was not found")

        return controller
    }

    fun cleanupControllers() {
        controllers.forEach {
            it.cleanup()
        }
    }
}