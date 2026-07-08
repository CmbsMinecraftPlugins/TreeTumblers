package xyz.devcmb.tumblers

import com.noxcrew.interfaces.InterfacesListeners
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json
import org.bukkit.plugin.java.JavaPlugin
import xyz.devcmb.tumblers.item.custom.ItemRegistry
import xyz.devcmb.tumblers.util.Font
import java.util.logging.Logger

class TreeTumblers : JavaPlugin() {
    companion object {
        lateinit var plugin: JavaPlugin
        lateinit var pluginLogger: Logger

        val pluginScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        const val NAMESPACE = "tumbling"
    }

    override fun onEnable() {
        plugin = this
        pluginLogger = logger

        Font.loadFontIndex()
        ItemRegistry.registerItems()

        InterfacesListeners.install(this)
        ControllerRegistry.init()
    }

    override fun onDisable() {
        ControllerRegistry.cleanup()
        pluginScope.cancel()
    }
}
