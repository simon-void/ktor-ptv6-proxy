package de.gmx.simonvoid.ptv6proxy.util

import java.net.URI
import io.ktor.client.*
import io.ktor.client.engine.apache5.*
import org.apache.hc.core5.http.HttpHost
import kotlin.time.Duration


/**
 * Initialize a HttpClient with the Apache5 engine because it worked best in another project.
 * But it might be time to reevaluate this choice.
 */
fun initHttpClient(
    proxyUrl: String?,
    timeout: Duration,
): HttpClient =
    HttpClient(Apache5) {
        engine {
            // this: Apache5EngineConfig
            followRedirects = true
            socketTimeout = timeout.inWholeMilliseconds.toInt()
            connectTimeout = timeout.inWholeMilliseconds
            connectionRequestTimeout = timeout.inWholeMilliseconds
            if (proxyUrl != null) {
                val proxyUri = URI(proxyUrl)
                val host = proxyUri.host ?: error("host is null in proxyUrl: $proxyUrl")
                val port = proxyUri.port.takeIf { it > 0 } ?: error("port is null in proxyUrl: $proxyUrl")
                customizeClient {
                    // this: HttpAsyncClientBuilder
                    setProxy(HttpHost(host, port))
                }
            }
        }
    }
