package de.gmx.simonvoid.ptv6proxy

import de.gmx.simonvoid.ptv6proxy.util.normalizeXML
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalAuthenticateModTest {

    val extAuthXMLWithSignatureTypeEcdsa = """
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
            <soap:Body>
                <ns4:ExternalAuthenticate xmlns="http://ws.gematik.de/conn/ConnectorCommon/v5.0"
                                          xmlns:ns2="http://ws.gematik.de/conn/ConnectorContext/v2.0"
                                          xmlns:ns3="urn:oasis:names:tc:dss:1.0:core:schema"
                                          xmlns:ns4="http://ws.gematik.de/conn/SignatureService/v7.4"
                >
                    <CardHandle>some</CardHandle>
                    <ns2:Context>
                        <MandantId>someMandantId</MandantId>
                        <ClientSystemId>someClientSystemId</ClientSystemId>
                        <WorkplaceId>someWorkplaceId</WorkplaceId>
                        <UserId>User1</UserId>
                    </ns2:Context>
                    <ns4:OptionalInputs>
                        <ns3:SignatureType>urn:bsi:tr:03111:ecdsa</ns3:SignatureType>
                    </ns4:OptionalInputs>
                    <ns4:BinaryString>
                        <ns3:Base64Data MimeType="application/octet-stream">DFKhFab/LDCfZTmy99sII3oJpEoJmZ+6j/ZxwouuPI0=
                        </ns3:Base64Data>
                    </ns4:BinaryString>
                </ns4:ExternalAuthenticate>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()

    @Test
    fun `test external authenticate without OptionalInputs tag gets OptionalInputs with ECDSA signature type`() {
        val externalAuthXMLWithoutCryptTag = """
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
            <soap:Body>
                <ns4:ExternalAuthenticate xmlns="http://ws.gematik.de/conn/ConnectorCommon/v5.0"
                                          xmlns:ns2="http://ws.gematik.de/conn/ConnectorContext/v2.0"
                                          xmlns:ns3="urn:oasis:names:tc:dss:1.0:core:schema"
                                          xmlns:ns4="http://ws.gematik.de/conn/SignatureService/v7.4"
                >
                    <CardHandle>some</CardHandle>
                    <ns2:Context>
                        <MandantId>someMandantId</MandantId>
                        <ClientSystemId>someClientSystemId</ClientSystemId>
                        <WorkplaceId>someWorkplaceId</WorkplaceId>
                        <UserId>User1</UserId>
                    </ns2:Context>
                    <ns4:BinaryString>
                        <ns3:Base64Data MimeType="application/octet-stream">DFKhFab/LDCfZTmy99sII3oJpEoJmZ+6j/ZxwouuPI0=
                        </ns3:Base64Data>
                    </ns4:BinaryString>
                </ns4:ExternalAuthenticate>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()

        val modifiedExternalAuthXML = ExternalAuthenticateMod.modifyBody(externalAuthXMLWithoutCryptTag.toByteArray())

        assertEquals(extAuthXMLWithSignatureTypeEcdsa.normalizeXML(), modifiedExternalAuthXML.normalizeXML())
    }

    @ParameterizedTest
    @ValueSource(strings = ["PKCS1", "ECDSA"])
    fun `test external authenticate with signatureType gets type ECDSA and no SignatureSchemes`(signatureType: String) {
        val givenExternalAuthXmlWithCryptTag = getExternalAuthXmlWithSigSchemesAndSigType(SignatureType.valueOf(signatureType))
        val modifiedExternalAuthXML = ExternalAuthenticateMod.modifyBody(givenExternalAuthXmlWithCryptTag.toByteArray())
        assertEquals(extAuthXMLWithSignatureTypeEcdsa.normalizeXML(), modifiedExternalAuthXML.normalizeXML())
    }

    @Test
    fun `test external authenticate doesApply`() {
        with(ExternalAuthenticateMod) {
            assertTrue(this.doesApply("http://ws.gematik.de/conn/SignatureService/v7.4#ExternalAuthenticate"))
            assertTrue(this.doesApply("http://ws.gematik.de/conn/SignatureService/v8.0.2#ExternalAuthenticate"))

            assertFalse(this.doesApply("http://ws.gematik.de/conn/EncryptionService/v7.4#ExternalAuthenticate"))
            assertFalse(this.doesApply("http://ws.gematik.de/co/SignatureService/v7.5#ExternalAuthenticate"))
            assertFalse(this.doesApply("http://ws.gematik.de/conn/SignatureService/v7.5#ExternalAuth"))
        }
    }

    private fun getExternalAuthXmlWithSigSchemesAndSigType(signatureType: SignatureType): String = """
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
            <soap:Body>
                <ns4:ExternalAuthenticate xmlns="http://ws.gematik.de/conn/ConnectorCommon/v5.0"
                                          xmlns:ns2="http://ws.gematik.de/conn/ConnectorContext/v2.0"
                                          xmlns:ns3="urn:oasis:names:tc:dss:1.0:core:schema"
                                          xmlns:ns4="http://ws.gematik.de/conn/SignatureService/v7.4"
                >
                    <CardHandle>some</CardHandle>
                    <ns2:Context>
                        <MandantId>someMandantId</MandantId>
                        <ClientSystemId>someClientSystemId</ClientSystemId>
                        <WorkplaceId>someWorkplaceId</WorkplaceId>
                        <UserId>User1</UserId>
                    </ns2:Context>
                    <ns4:OptionalInputs>
                        <ns3:SignatureType>${signatureType.value}</ns3:SignatureType>
                        <ns4:SignatureSchemes>RSASSA-PSS</ns4:SignatureSchemes>
                    </ns4:OptionalInputs>
                    <ns4:BinaryString>
                        <ns3:Base64Data MimeType="application/octet-stream">DFKhFab/LDCfZTmy99sII3oJpEoJmZ+6j/ZxwouuPI0=
                        </ns3:Base64Data>
                    </ns4:BinaryString>
                </ns4:ExternalAuthenticate>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()

    enum class SignatureType(val value: String) {
        PKCS1("urn:ietf:rfc:3447"),
        ECDSA("urn:bsi:tr:03111:ecdsa");
    }
}