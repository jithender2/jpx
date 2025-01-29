package com.proxy.beautifier;

import java.io.StringWriter;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import java.io.ByteArrayInputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import org.w3c.dom.Node;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.NodeList;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import org.xml.sax.InputSource;
import com.proxy.utils.BodyType;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import java.nio.charset.Charset;

/**
 * `XmlBeautifier` is a `Beautifier` implementation that formats XML content
 * into a more readable, indented format.  It uses standard Java XML libraries
 * (DOM and Transformer) for parsing and formatting.
 */
public class XmlBeautifier implements Beautifier {

	/**
	 * Checks if this beautifier accepts the given `BodyType`.
	 *
	 * @param type The `BodyType` to check.
	 * @return `true` if the `BodyType` is `BodyType.xml`, `false` otherwise.
	 */
	@Override
	public boolean accept(BodyType type) {
		return type == BodyType.xml;
	}

	/**
	 * Beautifies the given XML string, adding indentation and removing
	 * whitespace outside of tags.  If an error occurs during parsing or
	 * formatting, the original XML string is returned.
	 *
	 * @param s       The XML string to beautify.
	 * @param charset The character set of the XML string (used for parsing).
	 * @return The beautified XML string, or the original string if an error
	 *         occurs.
	 */
	@Override
	public String beautify(String s, Charset charset) {
		s = s.trim();
		String xml;
		try {
			xml = formatXML(s);
		} catch (Exception e) {

			xml = s;
		}
		return xml;
	}

	/**
	 * Formats the given XML string using DOM and Transformer.
	 *
	 * @param xml     The XML string to format.
	 * @param charset The character set of the XML string.
	 * @return The formatted XML string.
	 * @throws Exception If an error occurs during parsing or transformation.
	 */
	private String formatXML(String xml) throws Exception {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));

		// Remove whitespaces outside tags
		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']", document,
				XPathConstants.NODESET);

		for (int i = 0; i < nodeList.getLength(); ++i) {
			Node node = nodeList.item(i);
			node.getParentNode().removeChild(node);
		}

		// Setup pretty print options
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformerFactory.setAttribute("indent-number", 4);
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		if (!xml.startsWith("<?")) {
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		}
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		// Return pretty print xml string
		StringWriter stringWriter = new StringWriter();
		transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
		return stringWriter.toString();
	}
}