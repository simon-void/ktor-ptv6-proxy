package de.gmx.simonvoid.ptv6proxy

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.*
import kotlin.test.*
import org.junit.jupiter.api.Test
import kotlin.apply
import kotlin.to

class ApplicationTest {

    private val defaultTestConfig = MapApplicationConfig(
        listOf(
            "environment.connectorUrl" to "http://localhost:8090",
            "environment.timeout" to "1",
        )
    )

    @Test
    fun `test health endpoint`() = testApplication {
        environment {
            config = defaultTestConfig
        }
        application {
            module()
        }
        client.get("/Health").apply {
            assertEquals(HttpStatusCode.Companion.OK, status)
            assertEquals("I am healthy!", bodyAsText())
        }
    }
}