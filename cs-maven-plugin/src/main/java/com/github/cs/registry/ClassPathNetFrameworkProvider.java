package com.github.cs.registry;

import com.github.cs.FrameworkVersion;
import com.github.cs.NetFrameworkProvider;
import com.github.cs.Streams;
import com.github.cs.Xml;
import org.codehaus.plexus.component.annotations.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;

/**
 * TODO: document me
 *
 * @author miracelwhipp
 */
@Component(role = NetFrameworkProvider.class, hint = "default", instantiationStrategy = "singleton")
public class ClassPathNetFrameworkProvider implements NetFrameworkProvider {

	private FrameworkVersion version = null;

	@Override
	public File getFrameworkLibrary(String name) throws IOException {

		return Streams.getResourceFile(
				ClassPathNetFrameworkProvider.class, "build/" + getFrameworkVersion().versionedToken() + "/ref", name, "dll");
	}

	@Override
	public FrameworkVersion getFrameworkVersion() {

		try {

			if (version != null) {

				return version;
			}

			File resourceFile =
					Streams.getResourceFile(getClass(), null, "NETStandard.Library", "nuspec");

			if (resourceFile == null || !resourceFile.exists()) {

				return FrameworkVersion.defaultVersion();
			}

			Document nuSpec = Xml.parse(resourceFile, false);

			final String versionString = Xml.evaluateXpath(nuSpec, "/package/metadata/version");

			version = FrameworkVersion.fromShortName("netstandard" + versionString);

			return version;

		} catch (SAXException | XPathExpressionException | ParserConfigurationException | IOException e) {

			throw new IllegalStateException(e);
		}
	}
}
