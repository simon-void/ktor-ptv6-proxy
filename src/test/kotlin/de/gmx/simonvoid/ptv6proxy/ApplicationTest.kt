package de.gmx.simonvoid.ptv6proxy

import de.gmx.simonvoid.ptv6proxy.CheckCertificateExpirationModTest.Companion.checkCertExpXmlWithoutCryptTag
import de.gmx.simonvoid.ptv6proxy.CheckCertificateExpirationModTest.Companion.getCheckCertificateExpirationXmlWithCryptValue
import de.gmx.simonvoid.ptv6proxy.util.echoedRequestBody
import de.gmx.simonvoid.ptv6proxy.util.normalizeXML
import de.gmx.simonvoid.ptv6proxy.util.useMockServerWithEchoBodyResponse
import io.ktor.client.request.*
import io.ktor.client.statement.*
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

    @Test
    fun `test that the correct ModificationRule is applied if a correct soapAction header is present`() {
        useMockServerWithEchoBodyResponse(
            givenRequest = {
                withMethod("POST")
                withPath("/some_path")
            },
        ) { mockServerUrl ->
            testApplication {
                environment {
                    config = MapApplicationConfig(listOf(
                        "environment.connectorUrl" to mockServerUrl,
                        "environment.timeout" to "1",
                    ))
                }
                application {
                    module()
                }
                client.post("/some_path"){
                    contentType(ContentType.Application.Xml)
                    headers {
                        append("soapAction", "http://ws.gematik.de/conn/CertificateService/v6.0#CheckCertificateExpiration")
                    }
                    setBody(checkCertExpXmlWithoutCryptTag)
                }.let { response ->
                    val expectedXML = getCheckCertificateExpirationXmlWithCryptValue("ECC")
                    val actualXml = response.echoedRequestBody()

                    assertEquals(HttpStatusCode.fromValue(200), response.status)
                    assertEquals(expectedXML.normalizeXML(), actualXml.normalizeXML())
                }
            }
        }
    }

    @Test
    fun `test that ModificationRules are ignored if a correct soapAction header is absent`() {
        useMockServerWithEchoBodyResponse(
            givenRequest = {
                withMethod("POST")
                withPath("/some_path")
            },
        ) { mockServerUrl ->
            testApplication {
                environment {
                    config = MapApplicationConfig(listOf(
                        "environment.connectorUrl" to mockServerUrl,
                        "environment.timeout" to "1",
                    ))
                }
                application {
                    module()
                }
                client.post("/some_path"){
                    contentType(ContentType.Application.Xml)
                    headers {
                        append("soapAction", "http://ws.gematik.de/conn/CertificateService/v6.0#CheckCertificateExpirationWRONG")
                    }
                    setBody(checkCertExpXmlWithoutCryptTag)
                }.let { response ->
                    val expectedXML = checkCertExpXmlWithoutCryptTag
                    val actualXml = response.echoedRequestBody()

                    assertEquals(HttpStatusCode.fromValue(200), response.status)
                    assertEquals(expectedXML.normalizeXML(), actualXml.normalizeXML())
                }
            }
        }
    }
}
