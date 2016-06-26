package com.github.cs;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This abstract class works as base class for maven goals that compile c# code. It collects common parameters and
 * components.
 *
 * @author miracelwhipp
 */
public abstract class AbstractCompileCSharpMojo extends AbstractNetMojo {

	@Parameter
	protected List<String> preprocessorDefines;

	protected File compile(
			File workingDirectory,
			File csSourceDirectory,
			String outputFile,
			CSharpCompilerTargetType targetType,
			Set<String> allowedScopes,
			List<String> preprocessorDefines,
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

		for (File additionalReference : additionalReferences) {

			references.add(additionalReference);
		}

		CSharpCompiler compiler = new CSharpCompiler(
				getLog(),
				workingDirectory,
				csSourceDirectory,
				references,
				targetType,
				outputFile,
				preprocessorDefines,
				getFrameworkProvider()
		);

		return compiler.compile();
	}
}
