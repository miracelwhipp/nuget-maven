package com.github.cs.xunit;

import com.github.cs.NetTestRunner;
import com.github.cs.Streams;
import com.github.cs.TestExecutionException;
import com.github.cs.Xml;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * TODO: document me
 *
 * @author miracelwhipp
 */
public class XUnitTestRunner implements NetTestRunner {

	public static final String NET_472_SUB_DIRECTORY = "tools/net472";
	private final File workingDirectory;
	private final Logger logger;


	public XUnitTestRunner(File workingDirectory, Logger logger) {
		this.workingDirectory = workingDirectory;
		this.logger = logger;
	}

	@Override
	public void runTests(File testLibrary, List<String> includes, List<String> excludes, File resultFile) throws TestExecutionException {

		try {

			File completeResultFile = new File(resultFile.getParent(), "all-tests.xml");

			extractRunnerFiles(testLibrary.getParentFile());

			ProcessBuilder builder = new ProcessBuilder(new File(testLibrary.getParentFile(), "xunit.console.exe").getAbsolutePath());
			builder.directory(workingDirectory);

			builder.command().add(testLibrary.getAbsolutePath());

			for (String include : includes) {

				builder.command().add("-class");
				builder.command().add(include);
			}

			for (String exclude : excludes) {

				builder.command().add("-noclass");
				builder.command().add(exclude);
			}

			builder.command().add("-junit");
			builder.command().add(completeResultFile.getAbsolutePath());

			builder.inheritIO();

			Process process = builder.start();

			process.waitFor();

			int exitValue = process.exitValue();

			if (exitValue != 0) {

				logger.debug("xUnit runner finished with exit value " + exitValue);
				throw new TestExecutionException("xunit runner failed with exit value " + exitValue);
			}

			if (!completeResultFile.getAbsoluteFile().exists()) {

				throw new TestExecutionException("result file was not generated. file = " + completeResultFile.getAbsolutePath());
			}

			try (InputStream styleSheet = XUnitTestRunner.class.getClassLoader().getResourceAsStream("generate-result.xsl")) {

				Xml.transformFile(completeResultFile, new StreamSource(styleSheet), resultFile);
			}

		} catch (InterruptedException | IOException | ParserConfigurationException | SAXException | TransformerException e) {

			throw new TestExecutionException(e);
		}
	}

	private void extractRunnerFiles(File parentFile) throws TestExecutionException {

		try {

			Streams.loadResource(XUnitTestRunner.class, NET_472_SUB_DIRECTORY, "xunit.console.exe", parentFile);
			Streams.loadResource(XUnitTestRunner.class, NET_472_SUB_DIRECTORY, "xunit.abstractions.dll", parentFile);
			Streams.loadResource(XUnitTestRunner.class, NET_472_SUB_DIRECTORY, "xunit.runner.reporters.net452.dll", parentFile);
			Streams.loadResource(XUnitTestRunner.class, NET_472_SUB_DIRECTORY, "xunit.runner.utility.net452.dll", parentFile);

		} catch (IOException e) {

			throw new TestExecutionException(e);
		}

	}
}
