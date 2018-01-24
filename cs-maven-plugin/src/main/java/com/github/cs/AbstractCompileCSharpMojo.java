package com.github.cs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DependencyResolutionException;
import org.eclipse.aether.graph.Dependency;

/**
 * This abstract class works as base class for maven goals that compile c# code. It collects common parameters and
 * components.
 *
 * @author miracelwhipp
 */
public abstract class AbstractCompileCSharpMojo extends AbstractNetMojo {

	@Parameter
	protected List<String> frameworkReferences;

	@Parameter
	protected String platform;


	protected File compile(
			File workingDirectory,
			File csSourceDirectory,
			File generatedSourceDirectory,
			List<String> additionalSourceDirectories,
			String outputFile,
			CSharpCompilerTargetType targetType,
			Set<String> allowedScopes,
			List<String> preprocessorDefines,
			List<String> resources,
			File... additionalReferences
	) throws DependencyResolutionException, MojoFailureException {

		if (!workingDirectory.exists()) {

			workingDirectory.mkdirs();
		}

		if (!workingDirectory.isDirectory()) {

			throw new MojoFailureException("cannot execute compiler in " + workingDirectory.getAbsolutePath() + " - it is a file.");
		}

		List<Dependency> dllDependencies = getDllDependencies(allowedScopes);

		List<File> references = new ArrayList<>();

		for (Dependency dependency : dllDependencies) {

			references.add(dependency.getArtifact().getFile());
		}

		references.addAll(Arrays.asList(additionalReferences));

		List<File> sourceDirectories = new ArrayList<>();

		sourceDirectories.add(csSourceDirectory);
		sourceDirectories.add(generatedSourceDirectory);

		if (additionalSourceDirectories != null) {

			for (String directory : additionalSourceDirectories) {

				sourceDirectories.add(new File(directory));
			}
		}

		CSharpCompiler compiler = new CSharpCompiler(
				getLog(),
				workingDirectory,
				sourceDirectories,
				references,
				targetType,
				outputFile,
				platform,
				preprocessorDefines,
				getFrameworkProvider(),
				frameworkReferences,
				resources);

		return compiler.compile();
	}

}
