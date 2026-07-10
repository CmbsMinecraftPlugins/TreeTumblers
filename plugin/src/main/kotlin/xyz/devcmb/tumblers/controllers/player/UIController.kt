package xyz.devcmb.tumblers.controllers.player

import me.lucyydotp.tinsel.Tinsel
import me.lucyydotp.tinsel.font.FontFamily
import org.bukkit.NamespacedKey
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.IController
import kotlin.io.path.Path

@Controller
object UIController : IController {
    lateinit var tinsel: Tinsel
    override fun init() {
        tinsel = with(Tinsel.builder()) {
            val packFonts = FontFamily.fromResourcePack(Path(TreeTumblers.plugin.dataPath.toString(), "pack.zip"))
                .add(NamespacedKey(TreeTumblers.NAMESPACE, "spaces"))
                .add(NamespacedKey(TreeTumblers.NAMESPACE, "icons"))
                .add(NamespacedKey(TreeTumblers.NAMESPACE, "hud"))
                .add(NamespacedKey(TreeTumblers.NAMESPACE, "containers"))
                .add(NamespacedKey(TreeTumblers.NAMESPACE, "games"))
                .build()

            withFonts(packFonts)
            withTinselPack()

            build()
        }
    }
}