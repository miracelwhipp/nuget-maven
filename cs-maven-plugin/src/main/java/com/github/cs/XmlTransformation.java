package com.github.cs;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

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
import java.io.File;
import java.io.IOException;

/**
 * This utility class helps transforming xml files.
 *
 * @author miracelwhipp
 */
public final class XmlTransformation {

	private XmlTransformation() {
	}

	public static void transformFile(File sourceFile, Source styleSheet, File resultFile) throws ParserConfigurationException, IOException, SAXException, TransformerException {

		transformFile(sourceFile, styleSheet, resultFile, ParameterSetter.Identity);
	}

	public static void transformFile(File sourceFile, Source styleSheet, File resultFile, ParameterSetter parameterSetter) throws ParserConfigurationException, IOException, SAXException, TransformerException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		Document resultDocument = builder.parse(sourceFile);

		// Use a Transformer for output
		TransformerFactory tFactory = TransformerFactory.newInstance();

		Transformer transformer = tFactory.newTransformer(styleSheet);

		Source source = new DOMSource(resultDocument.getDocumentElement());
		Result result = new StreamResult(resultFile);

		parameterSetter.setParameters(transformer);

		transformer.transform(source, result);
	}

	public interface ParameterSetter {

		ParameterSetter Identity = new ParameterSetter() {
			@Override
			public void setParameters(Transformer transformer) {

			}
		};

		void setParameters(Transformer transformer);
	}
}
