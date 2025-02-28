package de.gmx.simonvoid.ptv6proxy

import de.gmx.simonvoid.ptv6proxy.util.defaultRoute
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.netty.*
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.*
import io.ktor.util.*
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
                val response = rasToEccService.handleRequest(call.request)
                call.respondWithResponse(response)
            }
        }
    }
}

data class Response(
    val statusCode: HttpStatusCode,
    val content: Content?,
) {
    companion object {
        fun fromText(text: String, contentType: ContentType = ContentType.Text.Plain, statusCode: HttpStatusCode): Response =
            Response(statusCode, Content(text.toByteArray(), contentType))

        suspend fun fromHttpResponse(res: HttpResponse, defaultContentType: ContentType): Response =
            Response(res.status, Content(res.readRawBytes(), res.contentType() ?: defaultContentType))
    }

    class Content(
        val bytes: ByteArray,
        val type: ContentType,
    ) {
        override fun toString() = "Content(type=$type, base64bytes=${bytes.encodeBase64()})"
    }
}

private suspend fun RoutingCall.respondWithResponse(response: Response) {
    if(response.content!=null) {
        val (bytes, contentType) = response.content.let { it.bytes to it.type }
        this.respondBytes(
            bytes = bytes,
            contentType = contentType,
            status = response.statusCode,
        )
    } else {
        this.response.status(response.statusCode)
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