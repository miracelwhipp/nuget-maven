package com.github.cs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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

	@Parameter(defaultValue = "")
	protected Map<String, String> frameworkProviderConfiguration;

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

	@Parameter(readonly = true, defaultValue = "${project.build.directory}")
	protected File workingDirectory;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	private RepositorySystemSession repoSession;

	@Component
	private ProjectDependenciesResolver projectDependenciesResolver;

	@Component
	protected NetFrameworkProvider frameworkProvider;

	@Parameter(readonly = true, defaultValue = "${project.artifactId}-${project.version}")
	private String outputFile;

//	protected synchronized NetFrameworkProvider getFrameworkProvider() {
//
//		if (frameworkProvider != null) {
//
//			return frameworkProvider;
//		}
//
//		frameworkProvider = loadFrameworkProvider();
//
//		return frameworkProvider;
//	}
//
//	private NetFrameworkProvider loadFrameworkProvider() {
//
//		ServiceLoader<NetFrameworkProviderFactory> serviceLoader =
//				ServiceLoader.load(NetFrameworkProviderFactory.class);
//
//		Iterator<NetFrameworkProviderFactory> providerIterator = serviceLoader.iterator();
//
//		Map<String, String> frameworkProviderConfiguration = this.frameworkProviderConfiguration;
//
//		if (frameworkProviderConfiguration == null) {
//
//			frameworkProviderConfiguration = Collections.emptyMap();
//		}
//
//		if (!providerIterator.hasNext()) {
//
//			//fall back to default provider, that expects the compiler executable to be in the path environment variable
//			return new DefaultNetFrameworkProvider(frameworkProviderConfiguration);
//		}
//
//		NetFrameworkProviderFactory result = providerIterator.next();
//
//		if (providerIterator.hasNext()) {
//
//			getLog().warn("several implementations for " + NetFrameworkProviderFactory.class.getCanonicalName() + " found. Using " + result.getClass().getCanonicalName());
//		}
//
//		return result.newFrameworkProvider(frameworkProviderConfiguration);
//	}

	protected List<Dependency> getDllDependencies(final Set<String> allowedScopes) throws DependencyResolutionException {

		return getDependencies(allowedScopes, Collections.singletonList("dll"));
	}

	public List<Dependency> getDependencies(final Set<String> allowedScopes, final List<String> allowedTypes) throws DependencyResolutionException {

		DefaultDependencyResolutionRequest request = new DefaultDependencyResolutionRequest();

		request.setMavenProject(project);

		request.setRepositorySession(repoSession);

		request.setResolutionFilter(new DependencyFilter() {
			@Override
			public boolean accept(DependencyNode dependencyNode, List<DependencyNode> list) {

				Dependency dependency = dependencyNode.getDependency();

				if (dependency == null) {

					return false;
				}

				if (!allowedScopes.contains(dependency.getScope())) {

					return false;
				}

				Artifact artifact = dependencyNode.getArtifact();

				String extension = artifact.getExtension();

				return allowedTypes.contains(extension);
			}
		});

		DependencyResolutionResult result = projectDependenciesResolver.resolve(request);

		List<Dependency> dependencies = result.getResolvedDependencies();

		return dependencies;
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
		String target = project.getArtifact().getArtifactHandler().getPackaging();

		return CSharpCompilerTargetType.fromString(target);
	}

	protected static File provideFile(Dependency testDependency, File targetDirectory) throws MojoFailureException {

		return provideFile(testDependency.getArtifact().getFile(), testDependency.getArtifact().getVersion(), targetDirectory);
	}

	protected static File provideFile(File file, String version, File targetDirectory) throws MojoFailureException {

		File targetFile = new File(targetDirectory,
				file.getName().replaceAll(Pattern.quote("-" + version), ""));

		if (!targetDirectory.exists()) {

			if (!targetDirectory.mkdirs()) {

				throw new MojoFailureException("unable to create directory " + targetDirectory.getAbsolutePath());
			}

		} else if (targetDirectory.isFile()) {

			throw new MojoFailureException("unable to create directory " + targetDirectory.getAbsolutePath() + ". It is a file.");
		}

		if (targetFile.exists()) {

			return targetFile;
		}

		try {

			Files.createLink(targetFile.toPath(), file.toPath());

			return targetFile;

		} catch (IOException e) {

			throw new MojoFailureException(e.getMessage(), e);
		}

//		try (InputStream source = new FileInputStream(file);
//		     OutputStream target = new FileOutputStream(new File(workingDirectory, file.getName()))) {
//
//			byte[] buffer = new byte[128 * 1024];
//
//			int length;
//
//			while ((length = source.read(buffer)) > 0) {
//
//				target.write(buffer, 0, length);
//			}
//		}
	}
}
