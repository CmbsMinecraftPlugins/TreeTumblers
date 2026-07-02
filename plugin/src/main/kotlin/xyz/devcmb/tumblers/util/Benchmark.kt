package xyz.devcmb.tumblers.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.devcmb.tumblers.TreeTumblers

class Benchmark(val id: String, val exec: suspend Benchmark.() -> Unit) {
    val completedSteps: ArrayList<String> = ArrayList()
    val timeStarted = System.nanoTime()
    var lastStepTime: Long? = null

    suspend fun run() {
        DebugUtil.benchmark("Started benchmarking for ID $id")
        exec()
    }

    fun completeStep(id: String) {
        completedSteps.add(id)
        DebugUtil.benchmark("Benchmark ${this.id} finished step $id in ${(System.nanoTime() - (lastStepTime ?: timeStarted)) / 1_000_000}ms (${(System.nanoTime() - (timeStarted)) / 1_000_000}ms total runtime)")
        lastStepTime = System.nanoTime()
    }

    fun yieldCompletion(ids: List<String>) {
        TreeTumblers.pluginScope.launch {
            while (!ids.all { completedSteps.contains(it) }) {
                delay(100)
            }

            DebugUtil.benchmark("Benchmark $id finished all steps in ${(System.nanoTime() - (timeStarted)) / 1_000_000}ms")
        }
    }
}