package xyz.devcmb.tumblers

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.plugin.PluginManager
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.ControllerBase
import xyz.devcmb.tumblers.util.DebugUtil

object ControllerRegistry : Listener {
    val controllers: HashMap<ControllerBase, Controller.Priority> = HashMap()

    /** Initialize all controllers **/
    @Suppress("UNCHECKED_CAST")
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, TreeTumblers.plugin)

        val reflections = Reflections(
            TreeTumblers::class.java.packageName,
            Scanners.TypesAnnotated
        )

        reflections.getTypesAnnotatedWith(Controller::class.java)
            .filter { ControllerBase::class.java.isAssignableFrom(it) }
            .sortedByDescending { it.getAnnotation(Controller::class.java).priority.value }
            .forEach { clazz ->
                val controllerClass = clazz as Class<out ControllerBase>
                val annotation = controllerClass.getAnnotation(Controller::class.java)

                val instance: ControllerBase =
                    controllerClass.getDeclaredConstructor().newInstance()

                register(instance, annotation)
            }
    }

    /** Register a single controller **/
    fun register(controller: ControllerBase, annotation: Controller) {
        val manager: PluginManager = Bukkit.getServer().pluginManager
        manager.registerEvents(controller, TreeTumblers.plugin)

        controllers[controller] = annotation.priority
        controller.init() // guess who forgot this :eyes:
        DebugUtil.success("Registered controller ${controller::class.java.simpleName} successfully")
    }

    /** Gets a controller by its type **/
    inline fun <reified T> getController(): T {
        val controller: T? = controllers.keys.find { it is T } as T?
        if(controller == null) throw IllegalArgumentException("Controller with the class ${T::class.java.simpleName} was not found")

        return controller
    }

    /** Lazily gets a controller to prevent race conditions with loading */
    inline fun <reified T> controller() : Lazy<T> = lazy { getController<T>() }

    /** Clean up all the controllers **/
    fun cleanup() {
        controllers.keys.forEach(ControllerBase::cleanup)
    }

    @EventHandler
    fun serverLoadEvent(event: ServerLoadEvent) {
        controllers
            .entries
            .sortedByDescending { it.value.value }
            .forEach { (controller, _) -> controller.serverLoad() }
    }
}