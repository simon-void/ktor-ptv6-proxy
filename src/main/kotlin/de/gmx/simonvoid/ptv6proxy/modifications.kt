package de.gmx.simonvoid.ptv6proxy

import de.gmx.simonvoid.ptv6proxy.util.*
import org.jdom2.Element

interface ModificationRule {
    fun doesApply(soapAction: String): Boolean
    fun modifyBody(xml: ByteArray): ByteArray
}

object CheckCertificateExpirationMod : ModificationRule {
    private val matcher = """http://ws.gematik.de/conn/CertificateService/v(\d|\.)+#CheckCertificateExpiration""".toRegex()
    override fun doesApply(soapAction: String): Boolean = matcher.matches(soapAction)

    override fun modifyBody(xml: ByteArray): ByteArray {
        val doc = parseDocument(xml)
        val checkCertExpElem: Element = doc.single("Envelope.Body.CheckCertificateExpiration")
        val cryptElem: Element? = checkCertExpElem.getChild("Crypt", checkCertExpElem.namespace)
        return if (cryptElem != null) {
            xml
        } else {
            val newCryptElem = Element("Crypt", checkCertExpElem.namespace).apply {
                text = "ECC"
            }
            checkCertExpElem.children.add(newCryptElem)
            doc.serialize(capacity = xml.size + 64)
        }
    }
}

object EncryptDocumentMod : ModificationRule {
    private val matcher = """http://ws.gematik.de/conn/EncryptionService/v(\d|\.)+#EncryptDocument""".toRegex()
    override fun doesApply(soapAction: String): Boolean = matcher.matches(soapAction)

    override fun modifyBody(xml: ByteArray): ByteArray {
        val doc = parseDocument(xml)
        val encryptDocumentElem: Element = doc.single("Envelope.Body.EncryptDocument")
        var nrOfInsertedCrypElems = 0
        encryptDocumentElem.children.filter { it.name.equals("RecipientKeys", ignoreCase = true) }.forEach { recipientKeysElem ->
            recipientKeysElem.children.filter { it.name.equals("CertificateOnCard", ignoreCase = true) }.forEach { certOnCardElem ->
                certOnCardElem.children.removeIf { it.name.equals("Crypt", ignoreCase = true) }
                certOnCardElem.children.add(Element("Crypt", encryptDocumentElem.namespace).apply {
                    text = "ECC"
                    nrOfInsertedCrypElems++
                })
            }
        }
        return doc.serialize(capacity = xml.size + (64 * nrOfInsertedCrypElems))
    }
}

object ExternalAuthenticateMod : ModificationRule {
    private val matcher = """http://ws.gematik.de/conn/SignatureService/v(\d|\.)+#ExternalAuthenticate""".toRegex()
    override fun doesApply(soapAction: String): Boolean = matcher.matches(soapAction)

    override fun modifyBody(xml: ByteArray): ByteArray {
        val doc = parseDocument(xml)
        val externalAuthElem: Element = doc.single("Envelope.Body.ExternalAuthenticate")
        val signatureTypeNamespace = run {
            val binaryStringElem = externalAuthElem.children.firstOrNull {
                it.name.equals("BinaryString", ignoreCase = true)
            }
            requireNotNull(binaryStringElem) {"Element ExternalAuthenticate must contain a BinaryString element but doesn't"}
            val base64DataElem = binaryStringElem.children.firstOrNull {
                it.name.equals("Base64Data", ignoreCase = true)
            }
            requireNotNull(base64DataElem) {"Element BinaryString must contain a Base64Data element but doesn't"}
            base64DataElem.namespace
        }
        externalAuthElem.children.removeIf { it.name.equals("OptionalInputs", ignoreCase = true) }
        externalAuthElem.addAfterChildWithName("Context", Element("OptionalInputs", externalAuthElem.namespace).apply {
            this.children.add(Element("SignatureType", signatureTypeNamespace).apply {
                this.text = "urn:bsi:tr:03111:ecdsa"
            })
        })
        return doc.serialize(capacity = xml.size + 64)
    }
}

object ReadCardCertificateMod : ModificationRule {
    private val matcher = """http://ws.gematik.de/conn/CertificateService/v(\d|\.)+#ReadCardCertificate""".toRegex()
    override fun doesApply(soapAction: String): Boolean = matcher.matches(soapAction)

    override fun modifyBody(xml: ByteArray): ByteArray {
        val doc = parseDocument(xml)
        val readCardCertElem: Element = doc.single("Envelope.Body.ReadCardCertificate")
        val cryptElem: Element? = readCardCertElem.getChild("Crypt", readCardCertElem.namespace)
        return if (cryptElem != null) {
            xml
        } else {
            val newCryptElem = Element("Crypt", readCardCertElem.namespace).apply {
                text = "ECC"
            }
            readCardCertElem.children.add(newCryptElem)
            doc.serialize(capacity = xml.size + 64)
        }
    }
}

object SignDocumentMod : ModificationRule {
    private val matcher = """http://ws.gematik.de/conn/SignatureService/v(\d|\.)+#SignDocument""".toRegex()
    override fun doesApply(soapAction: String): Boolean = matcher.matches(soapAction)

    override fun modifyBody(xml: ByteArray): ByteArray {
        val doc = parseDocument(xml)
        val signDocumentElem: Element = doc.single("Envelope.Body.SignDocument")
        val cryptElem: Element? = signDocumentElem.getChild("Crypt", signDocumentElem.namespace)
        if (cryptElem != null) {
            if(cryptElem.text=="RSA_ECC") return xml
            cryptElem.text = "RSA_ECC"
        } else {
            val newCryptElem = Element("Crypt", signDocumentElem.namespace).apply {
                text = "RSA_ECC"
            }
            signDocumentElem.addAfterChildWithName("CardHandle", newCryptElem)
        }
        return doc.serialize(capacity = xml.size + 64)
    }
}

