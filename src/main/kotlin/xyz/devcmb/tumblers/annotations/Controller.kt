package xyz.devcmb.tumblers.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Controller(
    val id: String,
    val priority: Priority = Priority.MEDIUM
) {
    enum class Priority(val value: Int) {
        LOWEST(0),
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        HIGHEST(4)
    }
}