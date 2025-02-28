package de.gmx.simonvoid.ptv6proxy

import de.gmx.simonvoid.ptv6proxy.util.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.util.*
import io.ktor.utils.io.*
import org.slf4j.Logger
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.measureTimedValue

class RsaToEccService(
    private val config: Config,
) {
    private val log: Logger = getLogger<RsaToEccService>().also { log ->
        log.info("using Config: $config")
    }
    private val client: HttpClient = initHttpClient(
        proxyUrl = config.proxyUrl,
        timeout = config.timeout,
    )
    private val modificationRules: List<ModificationRule> = listOf(
        SignDocumentMod,
        CheckCertificateExpirationMod,
        EncryptDocumentMod,
        ReadCardCertificateMod,
        ExternalAuthenticateMod,
    )

    suspend fun handleRequest(req: ApplicationRequest): Response {
        val startTimeNanos = System.nanoTime()
        // since this is a concurrent function, let's generate a random traceId to allow request tracing
        val traceId = TraceId.next()
        return try {
            val headers: Headers = req.headers
            log.info("$traceId ${req.httpMethod} received request with uri: ${req.uri} and headers: ${headers.toMap()}")

            val bodyToForward: ByteArray = run {
                val body: ByteArray = req.receiveChannel().toByteArray()
                headers["soapAction"]?.let{soapAction ->
                    val rule = modificationRules.firstOrNull { rule -> rule.doesApply(soapAction) }
                    if(rule==null) {
                        body
                    } else {
                        val modifiedBody = rule.modifyBody(body)
                        log.info("$traceId modified body for soapAction: $soapAction to base64: ${modifiedBody.encodeBase64()}")
                        modifiedBody.toByteArray(Charsets.UTF_8)
                    }
                } ?: body
            }

            val response: HttpResponse = client.postToConnector(headers, bodyToForward, traceId).getOrThrow()

            Response.fromHttpResponse( response, defaultContentType = ContentType.Application.Xml)

        } catch (e: Throwable) {
            log.error("$traceId request failed with exception: $e", e)
            Response.fromText(
                text = e.toString(),
                contentType = ContentType.Text.Plain,
                statusCode = HttpStatusCode.BadGateway,
            )
        } finally {
            val requestDuration = (System.nanoTime()-startTimeNanos).nanoseconds
            log.info("$traceId request finished after $requestDuration")
        }
    }

    private suspend fun HttpClient.postToConnector(
        allHeaders: Headers,
        body: ByteArray,
        traceId: TraceId,
    ): Result<HttpResponse> {
        val headers: Map<String, List<String>> = allHeaders.entries()
            .mapNotNull { (name, values ) ->
                if (name.equals("host", ignoreCase = true) ||
                    name.equals("Content-Length", ignoreCase = true)
                ) return@mapNotNull null
                name to values
            }.toMap()
        log.logPostRequestAsCurl(headers, body, traceId)

        val response: Result<HttpResponse> = run {
            val client = this
            val timedResponse = measureTimedValue {
                log.info("$traceId requesting POST ${config.connectorUrl}")
                runCatchingButAllowCancel {
                    client.post(config.connectorUrl) {
                        headers.forEach { (name, values) ->
                            if (!(name.equals("host", true) || name.equals("contentLength", true))) {
                                this.headers.appendAll(name, values)
                            }

                        }
                        setBody(body)
                    }
                }
            }
            timedResponse.value.onSuccess {
                log.info("$traceId POST response: {status: ${it.status}, version: ${it.version}, time: ${timedResponse.duration}}")
            }
        }
        return response
    }

    private fun Logger.logPostRequestAsCurl(headers: Map<String, List<String>>, body: ByteArray, traceId: TraceId) {
        val curlCommand = buildString {
            append("""$traceId equivalent curl query: echo "${body.encodeBase64()}" | base64 -d |curl -v -X POST""")
            config.proxyUrl?.let { append(" --proxy $it") }
            headers.forEach { name, values ->
                values.forEach { value ->
                    append(""" -H "$name: $value"""")
                }
            }
            append(" --data-binary @- ${config.connectorUrl}")
        }
        this.info(curlCommand)
    }
}
