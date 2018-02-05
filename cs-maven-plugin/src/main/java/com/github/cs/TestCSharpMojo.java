package com.github.cs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DependencyResolutionException;
import org.eclipse.aether.graph.Dependency;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * This goal executes the test for the c# code with the n-unit-3 console runner.
 *
 * @author miracelwhipp
 */
@Mojo(
		name = "test",
		defaultPhase = LifecyclePhase.TEST,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class TestCSharpMojo extends AbstractNetMojo {

	public static final String ENV_PATH = "Path";

	private static final Set<String> ALLOWED_SCOPES =
			Collections.unmodifiableSet(new HashSet<>(Arrays.asList("compile", "provided", "system", "test")));

	private static final String TEST_RESULT_FILE = "TestResult.xml";
	public static final String TRANSFORMATION_FILE = "nunit-to-junit.xsl";
	public static final int BUFFER_SIZE = 128 * 1024;
	public static final String RESULT_FILE = "result.xml";


	@Parameter(readonly = true, defaultValue = "${project.build.testOutputDirectory}")
	private File workingDirectory;

	@Parameter(readonly = true, defaultValue = "${project.artifactId}-${project.version}-tests.dll")
	private String testLibrary;

	@Parameter
	private List<String> includes = new ArrayList<>();

	@Parameter
	private List<String> excludes = new ArrayList<>();

	@Parameter(defaultValue = "false", property = "skipTests")
	private boolean skipTests;

	@Parameter(defaultValue = "${project.build.directory}/nunit-reports")
	private File reportsDirectory;

	@Parameter(defaultValue = "false", property = "maven.test.failure.ignore")
	private boolean ignoreFailure;

	@Parameter(readonly = true, defaultValue = "${project.artifactId}-${project.version}")
	private String outputFile;

	@Parameter(readonly = true, defaultValue = "${project.build.directory}")
	private File targetDirectory;

	@Parameter(defaultValue = "nunit")
	private String testRunnerHint;

	@Component
	private TestRunnerFactoryRegistry testRunnerFactoryRegistry;

	private NetTestRunnerFactory testRunnerFactory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {

			if (skipTests) {

				getLog().info("The tests are skipped.");

				return;
			}

			String targetString = project.getArtifact().getArtifactHandler().getPackaging();

			CSharpCompilerTargetType targetType = CSharpCompilerTargetType.fromString(targetString);

			if (targetType == null) {

				throw new MojoFailureException("unknown target type : " + targetString);
			}

			reportsDirectory.mkdirs();

//			try (
//					InputStream source =
//							TestCSharpMojo.class.getClassLoader().getResourceAsStream(TRANSFORMATION_FILE);
//					FileOutputStream target = new FileOutputStream(new File(reportsDirectory, TRANSFORMATION_FILE))
//			) {
//
//				int length = 0;
//				byte[] buffer = new byte[BUFFER_SIZE];
//
//				while ((length = source.read(buffer)) > 0) {
//
//					target.write(buffer, 0, length);
//				}
//			}


			File testLibraryFile = new File(workingDirectory, testLibrary);

			if (!testLibraryFile.exists()) {

				getLog().info("Test library file (" + testLibraryFile.getAbsolutePath() + ") does not exist. Tests execution skipped.");
				return;
			}

//			String condition = buildCondition(deNullify(includes), deNullify(excludes));
//
//			File runner = frameworkProvider.getNUnitRunner();
//
//			ProcessBuilder builder = new ProcessBuilder(runner.getPath());
//			builder.directory(workingDirectory);
//
//			builder.command().add(testLibraryFile.getAbsolutePath());
//
//			if (!condition.isEmpty()) {
//				builder.command().add("--where");
//				builder.command().add("\"" + condition + "\"");
//			}
//
//			builder.command().add("--result:" + new File(reportsDirectory, TEST_RESULT_FILE).getAbsolutePath());
//
//			provideFile(frameworkProvider.getNUnitLibrary());

			File assembly = new File(targetDirectory, outputFile + "." + targetType.getFileSuffix());

			provideFile(assembly, project.getVersion(), workingDirectory);

			List<Dependency> testDependencies = getTestDependencies();

			for (Dependency testDependency : testDependencies) {

				provideFile(testDependency, workingDirectory);
			}

//			builder.inheritIO();
//
//			Process process = builder.start();
//
//			process.waitFor();
//
//			int exitValue = process.exitValue();
//
//			if (exitValue == -2) {
//				// TODO: verify that this occurs if and only if the library does not contain tests
//				getLog().info("No tests found.");
//				return;
//			}
//
//			transformResultFile(exitValue);

			NetTestRunner testRunner = getTestRunnerFactory().newRunnerForDirectory(workingDirectory);

			File resultFile = new File(reportsDirectory, RESULT_FILE);

			testRunner.runTests(testLibraryFile, includes, excludes, resultFile);

//
//			if (exitValue != 0) {
//
//				getLog().debug("nUnit runner finished with exit value " + exitValue);

			if (!resultFile.exists() || resultFile.isDirectory()) {

				throw new MojoExecutionException("test results not found. expected file " + resultFile.getAbsolutePath());
			}

			Document document = XmlUtility.parse(resultFile);

			String numberOfFailures = XmlUtility.evaluateXpath(document.getDocumentElement(), "/failsafe-summary/@result");

			if (!numberOfFailures.trim().equals("0")) {

				if (!ignoreFailure) {
					throw new MojoFailureException("There were test failures.");
				}
			}


		} catch (IOException | TestExecutionException | DependencyResolutionException | SAXException | ParserConfigurationException | XPathExpressionException e) {

			throw new MojoFailureException("unable to run tests", e);
		}

	}

	private void transformResultFile(final int exitValue) throws MojoFailureException {

		try {
			final File styleSheet = new File(reportsDirectory, TRANSFORMATION_FILE);
			XmlUtility.transformFile(new File(reportsDirectory, TEST_RESULT_FILE), new StreamSource(styleSheet), new File(reportsDirectory, "result.xml"), new XmlUtility.ParameterSetter() {
				@Override
				public void setParameters(Transformer transformer) {
					transformer.setParameter("target-directory", reportsDirectory.getAbsolutePath());
					transformer.setParameter("nunit-result", exitValue);
				}
			});
		} catch (TransformerException | ParserConfigurationException | SAXException | IOException e) {

			throw new MojoFailureException("unable to transform file.", e);
		}

	}

	private List<Dependency> getTestDependencies() throws DependencyResolutionException {

		List<Dependency> testDependencies = getDependencies(ALLOWED_SCOPES, Arrays.asList("dll", "exe"));

		return testDependencies;
	}

	private List<String> deNullify(List<String> list) {

		if (list == null) {

			return Collections.emptyList();
		}

		return list;
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

	public synchronized NetTestRunnerFactory getTestRunnerFactory() {

		if (testRunnerFactory != null) {

			return testRunnerFactory;
		}

		NetTestRunnerFactory result = testRunnerFactoryRegistry.getFactory();

		testRunnerFactory = result;

		return testRunnerFactory;
	}

}
