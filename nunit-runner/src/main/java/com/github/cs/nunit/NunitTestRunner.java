package com.github.cs.nunit;

import com.github.cs.NetTestRunner;
import com.github.cs.TestExecutionException;
import com.github.cs.Xml;
import org.codehaus.plexus.logging.Logger;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * This {@link NetTestRunner} provides nunit3 as test runner
 *
 * @author miracelwhipp
 */
public class NunitTestRunner implements NetTestRunner {

	private static final String TEST_RESULT_FILE = "TestResult.xml";
	private static final String TRANSFORMATION_FILE = "nunit-to-junit.xsl";
	private static final int BUFFER_SIZE = 128 * 1024;

	private static final byte[] EMPTY_SUCCESS = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<failsafe-summary result=\"0\" timeout=\"false\">\n" +
			"   <completed>0</completed>\n" +
			"   <errors>0</errors>\n" +
			"   <failures>0</failures>\n" +
			"   <skipped>0</skipped>\n" +
			"</failsafe-summary>").getBytes(StandardCharsets.UTF_8);

	private final File workingDirectory;
	private final Logger logger;

	public NunitTestRunner(File workingDirectory, Logger logger) {
		this.workingDirectory = workingDirectory;
		this.logger = logger;
	}

	@Override
	public void runTests(File testLibrary, List<String> includes, List<String> excludes, File resultFile) throws TestExecutionException {

		File reportsDirectory = resultFile.getParentFile();

		try (
				InputStream source =
						NunitTestRunner.class.getClassLoader().getResourceAsStream(TRANSFORMATION_FILE);
				FileOutputStream target = new FileOutputStream(new File(reportsDirectory, TRANSFORMATION_FILE))
		) {

			int length = 0;
			byte[] buffer = new byte[BUFFER_SIZE];

			while ((length = source.read(buffer)) > 0) {

				target.write(buffer, 0, length);
			}

			target.close();

			String condition = buildCondition(includes, excludes);

			File executableFile = determineExecutableFile();

			ProcessBuilder builder = new ProcessBuilder(executableFile.getPath());
			builder.directory(workingDirectory);

			builder.command().add(testLibrary.getAbsolutePath());

			if (!condition.isEmpty()) {
				builder.command().add("--where");
				builder.command().add("\"" + condition + "\"");
			}

			builder.command().add("--result:" + new File(reportsDirectory, TEST_RESULT_FILE).getAbsolutePath());

			builder.inheritIO();

			Process process = builder.start();

			process.waitFor();

			int exitValue = process.exitValue();

			if (exitValue != 0) {

				logger.debug("nUnit runner finished with exit value " + exitValue);
			}

			if (exitValue == -2) {
				// TODO: verify that this occurs if and only if the library does not contain tests
				logger.info("No tests found.");

				try (FileOutputStream result = new FileOutputStream(resultFile)) {

					result.write(EMPTY_SUCCESS);
				}

				return;
			}

			transformResultFile(reportsDirectory, exitValue, resultFile);

		} catch (IOException | InterruptedException | TransformerException | SAXException | ParserConfigurationException e) {

			throw new TestExecutionException("could not execute tests", e);
		}
	}

	private File determineExecutableFile() throws TestExecutionException {

		File[] files = workingDirectory.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {

				if (!pathname.getName().startsWith("nunit3-console")) {

					return false;
				}

				return pathname.getName().endsWith(".exe");
			}
		});

		if (files == null || files.length == 0) {

			throw new TestExecutionException("no nunit 3 console runner found.");
		}

		return files[0];
	}

	private void transformResultFile(final File reportsDirectory, final int exitValue, File resultFile) throws ParserConfigurationException, TransformerException, SAXException, IOException {

		final File styleSheet = new File(reportsDirectory, TRANSFORMATION_FILE);

		Xml.transformFile(new File(reportsDirectory, TEST_RESULT_FILE), new StreamSource(styleSheet), resultFile, true, new Xml.ParameterSetter() {
			@Override
			public void setParameters(Transformer transformer) {
				transformer.setParameter("target-directory", reportsDirectory.getAbsolutePath());
				transformer.setParameter("nunit-result", exitValue);
			}
		});
	}


	private String buildCondition(List<String> includes, List<String> excludes) {

		StringBuilder result = new StringBuilder();

		appendIncludes(includes, result);

		appendExcludes(excludes, result);

		return result.toString();
	}

	private void appendExcludes(List<String> excludes, StringBuilder result) {

		if (excludes.isEmpty()) {

			return;
		}

		boolean first = true;

		for (String exclude : excludes) {

			if (first) {
				first = false;
			} else {
				result.append(" and ");
			}

			result.append("test !~ '");
			result.append(exclude);
			result.append("'");
		}

	}

	private void appendIncludes(List<String> includes, StringBuilder result) {

		if (includes.isEmpty()) {
			return;
		}

		result.append("(");

		boolean first = true;

		for (String include : includes) {

			if (first) {
				first = false;
			} else {
				result.append(" or ");
			}

			result.append("test =~ '");
			result.append(include);
			result.append("'");
		}

		result.append(")");
	}
}
