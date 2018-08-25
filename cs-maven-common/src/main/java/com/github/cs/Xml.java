package com.github.cs;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * This utility class helps transforming xml files.
 *
 * @author miracelwhipp
 */
public final class Xml {

	private Xml() {
	}


	public static void transformFile(File sourceFile, Source styleSheet, File resultFile) throws ParserConfigurationException, IOException, SAXException, TransformerException {

		transformFile(sourceFile, styleSheet, resultFile, true, ParameterSetter.Identity);
	}

	public static void transformFile(File sourceFile, Source styleSheet, File resultFile, boolean namespaceAware, ParameterSetter parameterSetter) throws ParserConfigurationException, IOException, SAXException, TransformerException {

		Document resultDocument = parse(sourceFile, namespaceAware);

		// Use a Transformer for output
		TransformerFactory tFactory = TransformerFactory.newInstance();

		Transformer transformer = tFactory.newTransformer(styleSheet);

		Source source = new DOMSource(resultDocument.getDocumentElement());
		Result result = new StreamResult(resultFile);

		parameterSetter.setParameters(transformer);

		transformer.transform(source, result);
	}

	public static Document parse(File sourceFile) throws ParserConfigurationException, SAXException, IOException {

		return parse(sourceFile, true);
	}

	public static Document parse(File sourceFile, boolean namespaceAware) throws ParserConfigurationException, SAXException, IOException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(namespaceAware);
		DocumentBuilder builder = factory.newDocumentBuilder();

		return builder.parse(sourceFile);
	}

	private static XPath getXpath(Node node) {

		XPath result = XPathFactory.newInstance().newXPath();

		result.setNamespaceContext(new NamespaceResolver(node.getOwnerDocument()));

		return result;
	}

	public static String evaluateXpath(Node node, String expression) throws XPathExpressionException {

		XPath xPath = getXpath(node);

		return xPath.evaluate(expression, node);
	}

	public static NodeList selectXpath(Node node, String expression) throws XPathExpressionException {

		XPath xPath = getXpath(node);

		return (NodeList) xPath.evaluate(expression, node, XPathConstants.NODESET);
	}

	public static Node selectUniqueXpath(Node node, String expression) throws XPathExpressionException {

		XPath xPath = getXpath(node);

		return (Node) xPath.evaluate(expression, node, XPathConstants.NODE);
	}

	public interface ParameterSetter {

		ParameterSetter Identity = new ParameterSetter() {
			@Override
			public void setParameters(Transformer transformer) {

			}
		};

		void setParameters(Transformer transformer);
	}

	private static class NamespaceResolver implements NamespaceContext {

		private final Document document;

		public NamespaceResolver(Document document) {
			this.document = document;
		}

		public String getNamespaceURI(String prefix) {
			if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
				return document.lookupNamespaceURI(null);
			} else {
				return document.lookupNamespaceURI(prefix);
			}
		}

		public String getPrefix(String namespaceURI) {
			return document.lookupPrefix(namespaceURI);
		}

		public Iterator<?> getPrefixes(String namespaceURI) {
			return null;
		}

	}
}
