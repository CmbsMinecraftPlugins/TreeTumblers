package xyz.devcmb.tumblers.controllers

import org.bukkit.Material
import org.bukkit.configuration.MemorySection
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.util.DebugUtil

@Controller("configController", priority = Controller.Priority.HIGHEST)
class ConfigController : IController {
    override fun init() {
        TreeTumblers.plugin.saveDefaultConfig()

        val reflections = Reflections(
            TreeTumblers::class.java.packageName,
            Scanners.FieldsAnnotated
        )

        reflections.getFieldsAnnotatedWith(Configurable::class.java).forEach { field ->
            field.isAccessible = true

            val annotation: Configurable = field.getAnnotation(Configurable::class.java)
            val config = TreeTumblers.plugin.config

            val path = annotation.path
            val value = if(!config.isSet(path)) null else when(field.type) {
                Int::class.java,
                Int::class.javaPrimitiveType -> config.getInt(path)

                Long::class.javaPrimitiveType,
                Long::class.java -> config.getLong(path)

                Double::class.javaPrimitiveType,
                Double::class.java -> config.getDouble(path)

                Float::class.javaPrimitiveType,
                Float::class.java -> config.getDouble(path).toFloat()

                String::class.java -> config.getString(path)

                MutableMap::class.java,
                HashMap::class.java -> config.getConfigurationSection(path)
                    ?.getKeys(false)
                    ?.associateWith { key ->
                        config.get("$path.$key")
                    }
                    ?.toMap(HashMap())
                    ?: HashMap<String, MemorySection>()

                Material::class.java -> Material.matchMaterial(config.getString(path)!!)

                else -> config.get(path)
            }

            if(value != null) {
                DebugUtil.info("Updated field ${field.name} to ${if(annotation.censor) "*".repeat(value.toString().length) else value} from config")
                field.set(null, value)
            } else {
                DebugUtil.warning("Path ${annotation.path} not found in config for field ${field.name}, using default value of ${field.get(null)}")
            }
        }
    }
}