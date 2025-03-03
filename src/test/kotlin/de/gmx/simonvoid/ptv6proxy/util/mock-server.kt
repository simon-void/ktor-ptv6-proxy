package de.gmx.simonvoid.ptv6proxy.util

import io.ktor.client.statement.*
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpTemplate
import org.mockserver.model.HttpTemplate.template

inline fun useMockServerWithEchoBodyResponse(
    isSecure: Boolean = false,
    host: String = "localhost",
    port: Int = 1080,
    crossinline givenRequest: HttpRequest.() -> Unit,
    crossinline test: (mockServerUrl: String) -> Unit,
) {
    /** Using @see[port] as local and remote port in the context of the mock server.
    It seems to work as intended, but I might not understand all the implications.*/
    ClientAndServer.startClientAndServer(host, port, port).use { mockServer: ClientAndServer ->
        mockServer.`when`(
            HttpRequest.request().apply{
                givenRequest()
                // setting this option automatically is more convenient, but a bit magic
                withSecure(isSecure)
            }
        ).respond(template(HttpTemplate.TemplateType.MUSTACHE, """
            {
                "statusCode": 200,
                "body": "{{ request.body }}"
            }
            """.trimIndent()
        ))

        val mockServerUrl = buildString {
            append(if (isSecure) "https" else "http")
            append("://$host:$port")
        }
        test(mockServerUrl)
    }
}

suspend fun HttpResponse.echoedRequestBody(): String {
    fun String.xmlUnescape(): String = this
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#x3D;", "=")

    return this.bodyAsText().xmlUnescape()
}