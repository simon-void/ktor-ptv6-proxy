package de.gmx.simonvoid.ptv6proxy.util

import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.input.DOMBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory


fun parseDocument(xmlBytes: ByteArray): Document {
    val domBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder()
    val jdomBuilder = DOMBuilder()

    val w3cDocument: org.w3c.dom.Document = domBuilder.parse(xmlBytes.inputStream())
    val jdomDocument: Document = jdomBuilder.build(w3cDocument)

    // alternatively
//    val saxBuilder = org.jdom2.input.SAXBuilder()
//    val jdomDocument: Document = saxBuilder.build(xmlBytes.inputStream())

    return jdomDocument
}

fun Document.serialize(format: Format = Format.getPrettyFormat()): String = XMLOutputter(format).outputString(this)

fun String.normalizeXML(): String {
    val doc = parseDocument(this.toByteArray())
    return doc.serialize(format = Format.getCompactFormat())
}

fun Document.single(path: String, separator: Char = '.'): Element {
    val names = path.split(separator).filter { it.isNotBlank() }
    fun Element.childWithName(name: String): Element {
        return this.children.singleOrNull() { child -> child.name == name } ?: error("Element with name ${this.name} does not contain unique element with name '$name'")
    }

    require(names.isNotEmpty()) {"at least one element in param names expected"}
    val namesIter = names.iterator()
    var element = this.rootElement
    check(element.name==namesIter.next()) {"root element has wrong name"}

    while (namesIter.hasNext()) {
        element = element.childWithName(namesIter.next())
    }
    return element
}

//fun Element.childByName(name: String): Element? = this.children.firstOrNull { it.name == name }

private fun Element.indexOfChildByName(name: String): Int? {
    this.children.forEachIndexed{ i, child -> if (child.name == name) return i }
    return null
}

fun Element.addAfterChildWithName(name: String, newChild: Element) {
    val antecessorIndex = indexOfChildByName(name)
        ?: error("Element ${this.name} did not contain required element $name, so new element is ${newChild.name} couldn't be placed after it")
    this.children.add(antecessorIndex+1, newChild)
}