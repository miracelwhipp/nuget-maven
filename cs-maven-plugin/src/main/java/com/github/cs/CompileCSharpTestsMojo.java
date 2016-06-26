package com.github.cs;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DependencyResolutionException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

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

	@Parameter(readonly = true, defaultValue = "${project.build.testOutputDirectory}")
	private File testOutputDirectory;

	@Parameter(readonly = true, defaultValue = "${project.artifactId}-${project.version}-tests")
	private String testOutputFile;

	private static final Set<String> ALLOWED_SCOPES =
			Collections.unmodifiableSet(new HashSet<>(Arrays.asList("compile", "provided", "system", "test")));


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {

			compile(
					testOutputDirectory,
					csTestSourceDirectory,
					testOutputFile,
					CSharpCompilerTargetType.LIBRARY,
					ALLOWED_SCOPES,
					preprocessorDefines,
					getFrameworkProvider().getNUnitLibrary()
			);

		} catch (DependencyResolutionException | IOException e) {

			throw new MojoFailureException(e.getMessage(), e);
		}

	}

}
