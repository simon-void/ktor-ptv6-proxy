package de.gmx.simonvoid.ptv6proxy

import de.gmx.simonvoid.ptv6proxy.util.normalizeXML
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignDocumentModTest {

    private val signDocXMLWithCryptTagRsaEcc = getSignDocXmlWithCryptValue("RSA_ECC")

    @Test
    fun `test sign document without crypt tag gets tag RSA_ECC`() {
        val signDocXMLWithoutCryptTag = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                <soap:Body>
                    <ns3:SignDocument xmlns="http://ws.gematik.de/conn/ConnectorCommon/v5.0" xmlns:ns2="http://ws.gematik.de/conn/ConnectorContext/v2.0" xmlns:ns3="http://ws.gematik.de/conn/SignatureService/v7.5" xmlns:ns5="urn:oasis:names:tc:dss:1.0:core:schema">
                        <CardHandle>someCardHandle</CardHandle>
                        <ns2:Context>
                            <MandantId>someMandantId</MandantId>
                            <ClientSystemId>someClientSystemId</ClientSystemId>
                            <WorkplaceId>someWorkplaceId</WorkplaceId>
                            <UserId>User1</UserId>
                        </ns2:Context>
                        <ns3:TvMode>NONE</ns3:TvMode>
                        <ns3:JobNumber>123</ns3:JobNumber>
                        <ns3:SignRequest RequestID="1">
                            <ns3:OptionalInputs>
                                <ns5:SignatureType>urn:ietf:rfc:5652</ns5:SignatureType>
                                <ns3:IncludeEContent>true</ns3:IncludeEContent>
                            </ns3:OptionalInputs>
                            <ns3:Document ShortText="E-Rezept Bundle Platzhalter">
                                <ns5:Base64Data MimeType="text/plain; charset=utf-8">cGxhY2Vob2xkZXI=</ns5:Base64Data>
                            </ns3:Document>
                            <ns3:IncludeRevocationInfo>false</ns3:IncludeRevocationInfo>
                        </ns3:SignRequest>
                    </ns3:SignDocument>
                </soap:Body>
            </soap:Envelope>
        """.trimIndent()

        val modifiedSignDocXML = SignDocumentMod.modifyBody(signDocXMLWithoutCryptTag.toByteArray())

        assertEquals(signDocXMLWithCryptTagRsaEcc.normalizeXML(), modifiedSignDocXML.normalizeXML())
    }

    @ParameterizedTest
    @ValueSource(strings = ["RSA_ECC", "RSA", "ECC"])
    fun `test sign document with crypt tag gets tag RSA_ECC`(cryptValue: String) {
        val givenSignDocXmlWithCryptTag = getSignDocXmlWithCryptValue(cryptValue)
        val modifiedSignDocXML = SignDocumentMod.modifyBody(givenSignDocXmlWithCryptTag.toByteArray())
        assertEquals(signDocXMLWithCryptTagRsaEcc.normalizeXML(), modifiedSignDocXML.normalizeXML())
    }

    @Test
    fun `test sign document doesApply`() {
        with(SignDocumentMod) {
            assertTrue(this.doesApply("http://ws.gematik.de/conn/SignatureService/v7.5#SignDocument"))
            assertTrue(this.doesApply("http://ws.gematik.de/conn/SignatureService/v8.0.1#SignDocument"))

            assertFalse(this.doesApply("http://ws.gematik.de/conn/EncryptionService/v6.1#EncryptDocument"))
            assertFalse(this.doesApply("http://ws.gematik.de/co/SignatureService/v7.5#SignDocument"))
            assertFalse(this.doesApply("http://ws.gematik.de/conn/SignatureService/v7.5#SignDoc"))
        }
    }

    private fun getSignDocXmlWithCryptValue(cryptValue: String): String = """
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
            <soap:Body>
                <ns3:SignDocument xmlns="http://ws.gematik.de/conn/ConnectorCommon/v5.0" xmlns:ns2="http://ws.gematik.de/conn/ConnectorContext/v2.0" xmlns:ns3="http://ws.gematik.de/conn/SignatureService/v7.5" xmlns:ns5="urn:oasis:names:tc:dss:1.0:core:schema">
                    <CardHandle>someCardHandle</CardHandle>
                    <ns3:Crypt>$cryptValue</ns3:Crypt>
                    <ns2:Context>
                        <MandantId>someMandantId</MandantId>
                        <ClientSystemId>someClientSystemId</ClientSystemId>
                        <WorkplaceId>someWorkplaceId</WorkplaceId>
                        <UserId>User1</UserId>
                    </ns2:Context>
                    <ns3:TvMode>NONE</ns3:TvMode>
                    <ns3:JobNumber>123</ns3:JobNumber>
                    <ns3:SignRequest RequestID="1">
                        <ns3:OptionalInputs>
                            <ns5:SignatureType>urn:ietf:rfc:5652</ns5:SignatureType>
                            <ns3:IncludeEContent>true</ns3:IncludeEContent>
                        </ns3:OptionalInputs>
                        <ns3:Document ShortText="E-Rezept Bundle Platzhalter">
                            <ns5:Base64Data MimeType="text/plain; charset=utf-8">cGxhY2Vob2xkZXI=</ns5:Base64Data>
                        </ns3:Document>
                        <ns3:IncludeRevocationInfo>false</ns3:IncludeRevocationInfo>
                    </ns3:SignRequest>
                </ns3:SignDocument>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
}