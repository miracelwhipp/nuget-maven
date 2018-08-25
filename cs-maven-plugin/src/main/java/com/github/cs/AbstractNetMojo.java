package com.github.cs;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.Dependency;

/**
 * This abstract class works as base class for c#/.net maven goals. It collects common components and parameters.
 *
 * @author miracelwhipp
 */
public abstract class AbstractNetMojo extends AbstractMojo {

	/**
	 * This parameter adds a classifier to the maven coordinates of the output file.
	 */
	@Parameter
	protected String classifier;

	@Parameter(readonly = true, defaultValue = "${project}")
	protected MavenProject project;
	/**
	 * This parameter specifies where generated sources are located.
	 */
	@Parameter(defaultValue = "${project.build.directory}/generated-sources/main/cs", property = "cs.generated.source.directory")
	protected File generatedSourceDirectory;
	/**
	 * Setting this parameter to true makes the types in this assembly visible to COM components.
	 */
	@Parameter(defaultValue = "false", property = "cs.assembly.com.visible")
	protected boolean comVisible;

	@Parameter(defaultValue = "")
	protected Map<String, String> frameworkProviderConfiguration;

	@Parameter(defaultValue = "default")
	protected String netFrameworkRoleHint;

	@Component
	protected NetFrameworkProvider frameworkProvider;

	@Parameter(defaultValue = "default")
	private String compilerRoleHint;

	@Parameter
	private Map<String, String> compilerProviderFactoryConfiguration;

	@Parameter(readonly = true, defaultValue = "${project.build.directory}")
	protected File workingDirectory;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	private RepositorySystemSession repoSession;

	@Component
	private ProjectDependenciesResolver projectDependenciesResolver;

	@Component
	protected CSharpCompilerProvider compilerProvider;

	@Parameter(readonly = true, defaultValue = "${project.artifactId}")
	private String outputFile;

	@Parameter
	private String targetType;

	public CSharpCompilerProvider getCompilerProvider() {

		return compilerProvider;
	}

	protected List<Dependency> getDllDependencies(final Set<String> allowedScopes) throws DependencyResolutionException {

		return getDependencies(allowedScopes, Collections.singletonList("dll"));
	}

	public List<Dependency> getDependencies(final Set<String> allowedScopes, final List<String> allowedTypes) throws DependencyResolutionException {

		return DependencyProvider.getDependencies(project, projectDependenciesResolver, repoSession, allowedScopes, allowedTypes);
	}


	protected String getClassifier() {
		return this.classifier != null ? this.classifier : "";
	}

	protected File getGeneratedSourceDirectory() {
		return new File(this.generatedSourceDirectory, "-" + getClassifier());
	}

	protected String getOutputFile() {

		String outputFile = this.outputFile;

		if (classifier != null) {
			outputFile = outputFile + "-" + classifier;
		}

		return outputFile;
	}

	protected CSharpCompilerTargetType getTargetType() {

		CSharpCompilerTargetType result = CSharpCompilerTargetType.fromString(targetType);

		if (result != null) {

			return result;
		}

		String target = project.getArtifact().getArtifactHandler().getPackaging();

		return CSharpCompilerTargetType.fromString(target);
	}

	public NetFrameworkProvider getFrameworkProvider() {

		return frameworkProvider;
	}
}
