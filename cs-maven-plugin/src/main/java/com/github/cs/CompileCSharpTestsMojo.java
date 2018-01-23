package com.github.cs;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DependencyResolutionException;

/**
 * This goal compiles the c# tests.
 *
 * @author miracelwhipp
 */
@Mojo(
		name = "test-compile",
		defaultPhase = LifecyclePhase.TEST_COMPILE,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class CompileCSharpTestsMojo extends AbstractCompileCSharpMojo {

	@Parameter(defaultValue = "${project.basedir}/src/test/cs", property = "cs.test.directory")
	private File csTestSourceDirectory;

	@Parameter(defaultValue = "${project.build.directory}/generated-sources/test/cs", property = "cs.generated.test.source.directory")
	private File generatedTestSourceDirectory;

	@Parameter
	protected List<String> preprocessorTestDefines = new ArrayList<>();

	@Parameter
	private List<String> additionalTestSourceDirectories;

	/**
	 * This parameter specifies files to be added as resources to the tests.
	 */
	@Parameter
	private List<String> testResources = new ArrayList<>();

	@Parameter(readonly = true, defaultValue = "${project.build.testOutputDirectory}")
	private File testOutputDirectory;

	@Parameter(readonly = true, defaultValue = "${project.artifactId}-${project.version}-tests")
	private String testOutputFile;

	@Parameter(readonly = true, defaultValue = "${project.artifactId}-${project.version}")
	private String outputFile;

	@Parameter(readonly = true, defaultValue = "${project.build.directory}")
	private File targetDirectory;


	private static final Set<String> ALLOWED_SCOPES =
			Collections.unmodifiableSet(new HashSet<>(Arrays.asList("compile", "provided", "system", "test")));


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		String target = project.getArtifact().getArtifactHandler().getPackaging();

		CSharpCompilerTargetType targetType = CSharpCompilerTargetType.fromString(target);

		if (targetType == null) {

			throw new MojoFailureException("unknown target type : " + target);
		}

		try {

			compile(
					testOutputDirectory,
					csTestSourceDirectory,
					generatedTestSourceDirectory,
					additionalTestSourceDirectories,
					testOutputFile,
					CSharpCompilerTargetType.LIBRARY,
					ALLOWED_SCOPES,
					preprocessorTestDefines,
					testResources,
					getFrameworkProvider().getNUnitLibrary(),
					new File(targetDirectory, outputFile + "." + targetType.getFileSuffix())
			);

		} catch (DependencyResolutionException | IOException e) {

			throw new MojoFailureException(e.getMessage(), e);
		}

	}

}
