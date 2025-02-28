package de.gmx.simonvoid.ptv6proxy.util

import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.utils.io.*
import org.koin.ext.getFullName
import org.slf4j.Logger
import kotlin.let
import kotlin.random.Random


@JvmInline
value class TraceId(private val value: Int) {
    override fun toString(): String = "(traceId: $value)"

    companion object {
        private val random = Random(System.currentTimeMillis())
        // since this is a concurrent function, let's generate a random traceId to allow request tracing
        // 5 digits should make collisions unlikely enough
        fun next() = TraceId(
            random.nextInt(
                from = 100_000,
                until = 1_000_000
            )
        )
    }
}

inline fun <T, R> T.runCatchingButAllowCancel(block: T.() -> R): Result<R> = try {
    Result.success(block())
} catch (e: Throwable) {
    if (e is CancellationException) throw e
    Result.failure(e)
}

inline fun <reified T> getLogger(): Logger = T::class.let { clazz ->
    clazz.simpleName?.let { KtorSimpleLogger(it) } ?: error("Couldn't get simple name for class " + clazz.getFullName())
}

inline fun Route.defaultRoute(crossinline handler: Route.()->Unit): Route = this.route("{...}") {
    handler()
}
