package xyz.devcmb.tumblers.annotations

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Configurable(val path: String)
