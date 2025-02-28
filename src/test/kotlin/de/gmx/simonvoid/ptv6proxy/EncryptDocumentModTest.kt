package de.gmx.simonvoid.ptv6proxy

import de.gmx.simonvoid.ptv6proxy.util.normalizeXML
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EncryptDocumentModTest {

    private val encryptDocXMLWithCryptTagEcc = getEncryptDocXmlWithCryptValue("ECC")

    @Test
    fun `test encrypt document without crypt tag gets tag RSA_ECC`() {
        val encryptDocXMLWithoutCryptTag = """
        <?xml version="1.0" encoding="UTF-8"?>
        <s11:Envelope xmlns:s11='http://schemas.xmlsoap.org/soap/envelope/'>
            <s11:Body>
                <CRYPT:EncryptDocument xmlns:CRYPT='http://ws.gematik.de/conn/EncryptionService/v6.1'>
                    <CCTX:Context xmlns:CCTX='http://ws.gematik.de/conn/ConnectorContext/v2.0'>
                        <CONN:MandantId xmlns:CONN='http://ws.gematik.de/conn/ConnectorCommon/v5.0'>
                            someMandantId
                        </CONN:MandantId>
                        <CONN:ClientSystemId xmlns:CONN='http://ws.gematik.de/conn/ConnectorCommon/v5.0'>
                            someClientSystemId
                        </CONN:ClientSystemId>
                        <CONN:WorkplaceId xmlns:CONN='http://ws.gematik.de/conn/ConnectorCommon/v5.0'>
                            someWorkplaceId
                        </CONN:WorkplaceId>
                        <CONN:UserId xmlns:CONN='http://ws.gematik.de/conn/ConnectorCommon/v5.0'>User1</CONN:UserId>
                    </CCTX:Context>
                    <CRYPT:RecipientKeys>
                        <CRYPT:CertificateOnCard>
                            <CONN:CardHandle xmlns:CONN='http://ws.gematik.de/conn/ConnectorCommon/v5.0'>someCardHandle
                            </CONN:CardHandle>
                        </CRYPT:CertificateOnCard>
                    </CRYPT:RecipientKeys>
                    <CONN:Document xmlns:CONN='http://ws.gematik.de/conn/ConnectorCommon/v5.0'>
                        <dss:Base64Data xmlns:dss='urn:oasis:names:tc:dss:1.0:core:schema'>cGxhY2Vob2xkZXI=</dss:Base64Data>
                    </CONN:Document>
                </CRYPT:EncryptDocument>
            </s11:Body>
        </s11:Envelope>
        """.trimIndent()

        val modifiedEncryptDocXML = EncryptDocumentMod.modifyBody(encryptDocXMLWithoutCryptTag.toByteArray())

        assertEquals(encryptDocXMLWithCryptTagEcc.normalizeXML(), modifiedEncryptDocXML.normalizeXML())
    }

    @ParameterizedTest
    @ValueSource(strings = ["RSA_ECC", "RSA", "ECC"])
    fun `test encrypt document with crypt tag gets tag ECC`(cryptValue: String) {
        val givenEncryptDocXmlWithCryptTag = getEncryptDocXmlWithCryptValue(cryptValue)
        val modifiedEncryptDocXML = EncryptDocumentMod.modifyBody(givenEncryptDocXmlWithCryptTag.toByteArray())
        assertEquals(encryptDocXMLWithCryptTagEcc.normalizeXML(), modifiedEncryptDocXML.normalizeXML())
    }

    @Test
    fun `test encrypt document doesApply`() {
        with(EncryptDocumentMod) {
            assertTrue(this.doesApply("http://ws.gematik.de/conn/EncryptionService/v6.1#EncryptDocument"))
            assertTrue(this.doesApply("http://ws.gematik.de/conn/EncryptionService/v7.0.2#EncryptDocument"))

            assertFalse(this.doesApply("http://ws.gematik.de/conn/SignatureService/v6.1#EncryptDocument"))
            assertFalse(this.doesApply("http://ws.gematik.de/co/EncryptionService/v7.5#EncryptDocument"))
            assertFalse(this.doesApply("http://ws.gematik.de/conn/EncryptionService/v7.5#EncryptDoc"))
        }
    }

    private fun getEncryptDocXmlWithCryptValue(cryptValue: String): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <s11:Envelope xmlns:s11='http://schemas.xmlsoap.org/soap/envelope/'>
            <s11:Body>
                <CRYPT:EncryptDocument xmlns:CRYPT='http://ws.gematik.de/conn/EncryptionService/v6.1'>
                    <CCTX:Context xmlns:CCTX='http://ws.gematik.de/conn/ConnectorContext/v2.0'>
                        <CONN:MandantId xmlns:CONN='http://ws.gematik.de/conn/ConnectorCommon/v5.0'>
                            someMandantId
                        </CONN:MandantId>
                        <CONN:ClientSystemId xmlns:CONN='http://ws.gematik.de/conn/ConnectorCommon/v5.0'>
                            someClientSystemId
                        </CONN:ClientSystemId>
                        <CONN:WorkplaceId xmlns:CONN='http://ws.gematik.de/conn/ConnectorCommon/v5.0'>
                            someWorkplaceId
                        </CONN:WorkplaceId>
                        <CONN:UserId xmlns:CONN='http://ws.gematik.de/conn/ConnectorCommon/v5.0'>User1</CONN:UserId>
                    </CCTX:Context>
                    <CRYPT:RecipientKeys>
                        <CRYPT:CertificateOnCard>
                            <CONN:CardHandle xmlns:CONN='http://ws.gematik.de/conn/ConnectorCommon/v5.0'>someCardHandle
                            </CONN:CardHandle>
                            <CRYPT:Crypt>$cryptValue</CRYPT:Crypt>
                        </CRYPT:CertificateOnCard>
                    </CRYPT:RecipientKeys>
                    <CONN:Document xmlns:CONN='http://ws.gematik.de/conn/ConnectorCommon/v5.0'>
                        <dss:Base64Data xmlns:dss='urn:oasis:names:tc:dss:1.0:core:schema'>cGxhY2Vob2xkZXI=</dss:Base64Data>
                    </CONN:Document>
                </CRYPT:EncryptDocument>
            </s11:Body>
        </s11:Envelope>
        """.trimIndent()
}