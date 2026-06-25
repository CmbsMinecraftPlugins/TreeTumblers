package xyz.devcmb.tumblers

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.plugin.PluginManager
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.IController
import xyz.devcmb.tumblers.util.DebugUtil

object ControllerRegistry : Listener {
    private val controllers: HashMap<IController, Controller.Priority> = HashMap()

    /** Initialize all controllers **/
    @Suppress("UNCHECKED_CAST")
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, TreeTumblers.plugin)

        val reflections = Reflections(
            TreeTumblers::class.java.packageName,
            Scanners.TypesAnnotated
        )

        reflections.getTypesAnnotatedWith(Controller::class.java)
            .filter { IController::class.java.isAssignableFrom(it) }
            .sortedByDescending { it.getAnnotation(Controller::class.java).priority.value }
            .forEach { clazz ->
                val controllerClass = clazz as Class<out IController>
                val annotation = controllerClass.getAnnotation(Controller::class.java)

                val instance: IController =
                    controllerClass.kotlin.objectInstance as IController

                register(instance, annotation)
            }
    }

    /** Register a single controller **/
    fun register(controller: IController, annotation: Controller) {
        val manager: PluginManager = Bukkit.getServer().pluginManager
        manager.registerEvents(controller, TreeTumblers.plugin)

        controllers[controller] = annotation.priority
        controller.init() // guess who forgot this :eyes:
        DebugUtil.success("Registered controller ${controller::class.java.simpleName} successfully")
    }

    /** Clean up all the controllers **/
    fun cleanup() {
        controllers.keys.forEach(IController::cleanup)
    }

    @EventHandler
    fun serverLoadEvent(event: ServerLoadEvent) {
        controllers
            .entries
            .sortedByDescending { it.value.value }
            .forEach { (controller, _) -> controller.serverLoad() }
    }
}