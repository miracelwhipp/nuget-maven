package com.github.cs;

import com.github.cs.compile.AssemblyFileProperties;
import com.github.cs.compile.CSharpCompiler;
import com.github.cs.compile.CSharpCompilerOptions;
import com.github.cs.compile.SourceFiles;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DependencyResolutionException;
import org.eclipse.aether.graph.Dependency;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * This abstract class works as base class for maven goals that compile c# code. It collects common parameters and
 * components.
 *
 * @author miracelwhipp
 */
public abstract class AbstractCompileCSharpMojo extends AbstractNetMojo {

	@Parameter
	protected List<String> frameworkReferences = Collections.singletonList("netstandard");

	@Parameter
	protected String platform;

	@Parameter(defaultValue = "false")
	protected boolean unsafe;

	protected File compile(
			File workingDirectory,
			File csSourceDirectory,
			File generatedSourceDirectory,
			List<String> additionalSourceDirectories,
			List<String> frameworkReferences,
			String outputFile,
			CSharpCompilerTargetType targetType,
			Set<String> allowedScopes,
			List<String> preprocessorDefines,
			List<String> resources,
			File keyfile,
			File... additionalReferences
	) throws DependencyResolutionException, MojoFailureException {

		return compile(
				workingDirectory,
				csSourceDirectory,
				generatedSourceDirectory,
				additionalSourceDirectories,
				frameworkReferences,
				outputFile,
				targetType,
				allowedScopes,
				preprocessorDefines,
				resources,
				keyfile,
				Arrays.asList(additionalReferences)
		);
	}

	protected File compile(
			File workingDirectory,
			File csSourceDirectory,
			File generatedSourceDirectory,
			List<String> additionalSourceDirectories,
			List<String> frameworkReferences,
			String outputFile,
			CSharpCompilerTargetType targetType,
			Set<String> allowedScopes,
			List<String> preprocessorDefines,
			List<String> resources,
			File keyfile,
			List<File> additionalReferences
	) throws DependencyResolutionException, MojoFailureException {

		try {

			if (!workingDirectory.exists()) {

				workingDirectory.mkdirs();
			}

			if (!workingDirectory.isDirectory()) {

				throw new MojoFailureException("cannot execute compiler in " + workingDirectory.getAbsolutePath() + " - it is a file.");
			}

			List<Dependency> dllDependencies = getDllDependencies(allowedScopes);

			List<File> references = new ArrayList<>();

			for (Dependency dependency : dllDependencies) {

				File referencedFile = DependencyProvider.provideFile(dependency, workingDirectory);
				references.add(referencedFile);
			}

			references.addAll(additionalReferences);

			List<File> sourceDirectories = new ArrayList<>();

			sourceDirectories.add(csSourceDirectory);
			sourceDirectories.add(generatedSourceDirectory);

			if (additionalSourceDirectories != null) {

				for (String directory : additionalSourceDirectories) {

					sourceDirectories.add(new File(directory));
				}
			}

			CSharpCompiler compiler = new CSharpCompiler(
					new SourceFiles(workingDirectory, sourceDirectories, references, frameworkReferences, resources, keyfile),
					new CSharpCompilerOptions(preprocessorDefines, unsafe),
					new AssemblyFileProperties(targetType, outputFile, platform),
					getLog(),
					getFrameworkProvider(),
					getCompilerProvider()
			);

			return compiler.compile();

		} catch (IOException e) {

			throw new MojoFailureException(e.getMessage(), e);
		}
	}

}
