package de.gmx.simonvoid.ptv6proxy

import de.gmx.simonvoid.ptv6proxy.util.normalizeXML
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadCardCertificateModTest {
    @Test
    fun `test Read Card Certificate without crypt tag gets tag ECC`() {
        val readCardCertXmlWithCryptTag = """
        <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"
                           xmlns:m0="http://ws.gematik.de/conn/ConnectorCommon/v5.0"
                           xmlns:m1="http://ws.gematik.de/conn/ConnectorContext/v2.0">
            <SOAP-ENV:Body>
                <m:ReadCardCertificate xmlns:m="http://ws.gematik.de/conn/CertificateService/v6.0">
                    <m0:CardHandle>someCardHandle</m0:CardHandle>
                    <m1:Context>
                        <m0:MandantId>someMandantId</m0:MandantId>
                        <m0:ClientSystemId>someClientSystemId</m0:ClientSystemId>
                        <m0:WorkplaceId>someWorkplaceId</m0:WorkplaceId>
                        <m0:UserId>User1</m0:UserId>
                    </m1:Context>
                    <m:CertRefList>
                        <m:CertRef>C.QES</m:CertRef>
                    </m:CertRefList>
                </m:ReadCardCertificate>
            </SOAP-ENV:Body>
        </SOAP-ENV:Envelope>
            """.trimIndent()

        val modifiedReadCardCertXML = ReadCardCertificateMod.modifyBody(readCardCertXmlWithCryptTag.toByteArray())

        assertEquals(getReadCardCertificateXmlWithCryptValue("ECC").normalizeXML(), modifiedReadCardCertXML.normalizeXML())
    }

    @ParameterizedTest
    @ValueSource(strings = ["RSA", "ECC"])
    fun `test Read Card Certificate with crypt tag retains that tag`(cryptValue: String) {
        val givenReadCardCertXmlWithCryptTag = getReadCardCertificateXmlWithCryptValue(cryptValue)
        val modifiedReadCardCertXML = ReadCardCertificateMod.modifyBody(givenReadCardCertXmlWithCryptTag.toByteArray())
        assertEquals(givenReadCardCertXmlWithCryptTag.normalizeXML(), modifiedReadCardCertXML.normalizeXML())
    }

    @Test
    fun `test Read Card Certificate doesApply`() {
        with(ReadCardCertificateMod) {
            assertTrue(this.doesApply("http://ws.gematik.de/conn/CertificateService/v6.0#ReadCardCertificate"))
            assertTrue(this.doesApply("http://ws.gematik.de/conn/CertificateService/v7.1.20#ReadCardCertificate"))

            assertFalse(this.doesApply("http://ws.gematik.de/conn/EncryptionService/v6.1#ReadCardCertificate"))
            assertFalse(this.doesApply("http://ws.gematik.de/co/CertificateService/v7.5#ReadCardCertificate"))
            assertFalse(this.doesApply("http://ws.gematik.de/conn/CertificateService/v7.5#ReadCardCert"))
        }
    }

    private fun getReadCardCertificateXmlWithCryptValue(cryptValue: String): String = """
        <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"
                           xmlns:m0="http://ws.gematik.de/conn/ConnectorCommon/v5.0"
                           xmlns:m1="http://ws.gematik.de/conn/ConnectorContext/v2.0">
            <SOAP-ENV:Body>
                <m:ReadCardCertificate xmlns:m="http://ws.gematik.de/conn/CertificateService/v6.0">
                    <m0:CardHandle>someCardHandle</m0:CardHandle>
                    <m1:Context>
                        <m0:MandantId>someMandantId</m0:MandantId>
                        <m0:ClientSystemId>someClientSystemId</m0:ClientSystemId>
                        <m0:WorkplaceId>someWorkplaceId</m0:WorkplaceId>
                        <m0:UserId>User1</m0:UserId>
                    </m1:Context>
                    <m:CertRefList>
                        <m:CertRef>C.QES</m:CertRef>
                    </m:CertRefList>
                    <m:Crypt>$cryptValue</m:Crypt>
                </m:ReadCardCertificate>
            </SOAP-ENV:Body>
        </SOAP-ENV:Envelope>
        """.trimIndent()
}