package de.gmx.simonvoid.ptv6proxy

import de.gmx.simonvoid.ptv6proxy.util.defaultRoute
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.netty.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    // see resources/application.yaml for configuration
    EngineMain.main(args)
}

fun Application.module() {
    val config: Config = initAppMode(this)
    install(Koin) {
        modules(
            module {
                single { RsaToEccService(config) }
            }
        )
    }

    val rasToEccService = inject<RsaToEccService>().value

    routing {
        get("Health") {
            call.respond("I am healthy!")
        }
        defaultRoute {
            handle {
                rasToEccService.handleRequest(call)
            }
        }
    }
}


private fun initAppMode(app: Application): Config {
    val config = app.environment.config
    run {
        // log config
        fun Map<String, Any?>.logContent(name: String) {
            fun logWithConcatKeys(keyPrefixes: List<String>, value: Any?) {
                when (value) {
                    is Map<*, *> -> value.forEach { key, subValue -> logWithConcatKeys(keyPrefixes+key.toString(), subValue) }
                    else -> app.log.info("$name: ${keyPrefixes.joinToString(".")}: $value")
                }
            }
            this.forEach { (key, value) -> logWithConcatKeys(listOf(key), value) }
        }
        config.toMap().logContent("config")
    }

    fun ApplicationConfig.getNonBlankStringPropertyOrNull(name: String): String? =
        this.propertyOrNull(name)?.getString()?.trim()?.ifEmpty { null }

    val connectorUrl: String? = config.getNonBlankStringPropertyOrNull("environment.connectorUrl")
    val proxyUrl: String? = config.getNonBlankStringPropertyOrNull("environment.proxyUrl")
    val timeout: Duration = config.getNonBlankStringPropertyOrNull( "environment.timeout").let{
        it?.toLongOrNull()?.seconds ?: error("invalid value for TIMEOUT_IN_SEC: $it is not a Long")
    }

    return Config(
        connectorUrl = connectorUrl ?: error("""missing environment variable: "PTV5PLUS_CONNECTOR_URL""""),
        proxyUrl = proxyUrl,
        timeout = timeout,
    )
}

data class Config(
    val connectorUrl: String,
    val proxyUrl: String? = null,
    val timeout: Duration,
)