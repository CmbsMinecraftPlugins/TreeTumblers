package xyz.devcmb.tumblers.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import org.bukkit.Bukkit
import xyz.devcmb.tumblers.TreeTumblers
import kotlin.coroutines.CoroutineContext

object BukkitDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if(Bukkit.isPrimaryThread()) {
            block.run()
            return
        }

        Bukkit.getScheduler().runTask(TreeTumblers.plugin, block)
    }
}