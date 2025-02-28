package de.gmx.simonvoid.ptv6proxy

import de.gmx.simonvoid.ptv6proxy.util.normalizeXML
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckCertificateExpirationModTest {
    @Test
    fun `test check Certificate Expiration without crypt tag gets tag ECC`() {
        val checkCertExpXmlWithCryptTag = """
            <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"
                               xmlns:m0="http://ws.gematik.de/conn/ConnectorCommon/v5.0"
                               xmlns:m1="http://ws.gematik.de/conn/ConnectorContext/v2.0">
                <SOAP-ENV:Body>
                    <m:CheckCertificateExpiration xmlns:m="http://ws.gematik.de/conn/CertificateService/v6.0">
                        <m0:CardHandle>someCardHandle</m0:CardHandle>
                        <m1:Context>
                            <m0:MandantId>someMandantId</m0:MandantId>
                            <m0:ClientSystemId>someClientSystemId</m0:ClientSystemId>
                            <m0:WorkplaceId>someWorkplaceId</m0:WorkplaceId>
                            <m0:UserId>User1</m0:UserId>
                        </m1:Context>
                    </m:CheckCertificateExpiration>
                </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
            """.trimIndent()

        val modifiedCheckCertExpXML = CheckCertificateExpirationMod.modifyBody(checkCertExpXmlWithCryptTag.toByteArray())

        assertEquals(getCheckCertificateExpirationXmlWithCryptValue("ECC").normalizeXML(), modifiedCheckCertExpXML.normalizeXML())
    }

    @ParameterizedTest
    @ValueSource(strings = ["RSA", "ECC"])
    fun `test Check Certificate Expiration with crypt tag retains that tag`(cryptValue: String) {
        val givenCheckCertExpXmlWithCryptTag = getCheckCertificateExpirationXmlWithCryptValue(cryptValue)
        val modifiedCheckCertExpXML = CheckCertificateExpirationMod.modifyBody(givenCheckCertExpXmlWithCryptTag.toByteArray())
        assertEquals(givenCheckCertExpXmlWithCryptTag.normalizeXML(), modifiedCheckCertExpXML.normalizeXML())
    }

    @Test
    fun `test Check Certificate Expiration doesApply`() {
        with(CheckCertificateExpirationMod) {
            assertTrue(this.doesApply("http://ws.gematik.de/conn/CertificateService/v6.0#CheckCertificateExpiration"))
            assertTrue(this.doesApply("http://ws.gematik.de/conn/CertificateService/v7.1.20#CheckCertificateExpiration"))

            assertFalse(this.doesApply("http://ws.gematik.de/conn/EncryptionService/v6.1#EncryptDocument"))
            assertFalse(this.doesApply("http://ws.gematik.de/co/CertificateService/v7.5#CheckCertificateExpiration"))
            assertFalse(this.doesApply("http://ws.gematik.de/conn/CertificateService/v7.5#CheckCertificateExpir"))
        }
    }

    private fun getCheckCertificateExpirationXmlWithCryptValue(cryptValue: String): String = """
        <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"
                           xmlns:m0="http://ws.gematik.de/conn/ConnectorCommon/v5.0"
                           xmlns:m1="http://ws.gematik.de/conn/ConnectorContext/v2.0">
            <SOAP-ENV:Body>
                <m:CheckCertificateExpiration xmlns:m="http://ws.gematik.de/conn/CertificateService/v6.0">
                    <m0:CardHandle>someCardHandle</m0:CardHandle>
                    <m1:Context>
                        <m0:MandantId>someMandantId</m0:MandantId>
                        <m0:ClientSystemId>someClientSystemId</m0:ClientSystemId>
                        <m0:WorkplaceId>someWorkplaceId</m0:WorkplaceId>
                        <m0:UserId>User1</m0:UserId>
                    </m1:Context>
                    <m:Crypt>$cryptValue</m:Crypt>
                </m:CheckCertificateExpiration>
            </SOAP-ENV:Body>
        </SOAP-ENV:Envelope>
        """.trimIndent()
}