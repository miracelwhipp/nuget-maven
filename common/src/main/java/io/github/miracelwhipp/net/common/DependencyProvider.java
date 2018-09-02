package io.github.miracelwhipp.net.common;

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
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This utility class collects methods to provide files in specific directories.
 *
 * @author miracelwhipp
 */
public final class DependencyProvider {

	private DependencyProvider() {
	}


	public static File provideFile(Dependency dependency, File targetDirectory) throws IOException {

		return provideFile(dependency.getArtifact().getFile(), dependency.getArtifact().getVersion(), targetDirectory);
	}

	public static File provideFile(File file, String version, File targetDirectory) throws IOException {

		File targetFile = version == null ? new File(targetDirectory, file.getName()) : new File(targetDirectory,
				file.getName().replaceAll(Pattern.quote("-" + version), ""));

		if (!targetDirectory.exists()) {

			if (!targetDirectory.mkdirs()) {

				throw new IOException("unable to create directory " + targetDirectory.getAbsolutePath());
			}

		} else if (targetDirectory.isFile()) {

			throw new IOException("unable to create directory " + targetDirectory.getAbsolutePath() + ". It is a file.");
		}

		if (targetFile.exists()) {

			return targetFile;
		}


		Files.createLink(targetFile.toPath(), file.toPath());

		return targetFile;

	}

	public static Dependency findDependency(
			MavenProject project,
			ProjectDependenciesResolver projectDependenciesResolver,
			RepositorySystemSession repoSession,
			final String groupId,
			final String artifactId
	) throws DependencyResolutionException {

		DefaultDependencyResolutionRequest request = new DefaultDependencyResolutionRequest();

		request.setMavenProject(project);

		request.setRepositorySession(repoSession);

		request.setResolutionFilter(new DependencyFilter() {
			@Override
			public boolean accept(DependencyNode node, List<DependencyNode> parents) {

				return node.getArtifact().getArtifactId().equals(artifactId) &&
						node.getArtifact().getGroupId().equals(groupId);
			}
		});

		DependencyResolutionResult result = projectDependenciesResolver.resolve(request);

		List<Dependency> dependencies = result.getResolvedDependencies();

		if (dependencies.isEmpty()) {

			return null;
		}

		return dependencies.get(0);

	}

	public static List<Dependency> getDependencies(
			MavenProject project,
			ProjectDependenciesResolver projectDependenciesResolver,
			RepositorySystemSession repoSession,
			final Set<String> allowedScopes,
			final List<String> allowedTypes
	) throws DependencyResolutionException {

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

				if (allowedScopes != null && !allowedScopes.contains(dependency.getScope())) {

					return false;
				}

				Artifact artifact = dependencyNode.getArtifact();

				String extension = artifact.getExtension();

				return allowedTypes != null && allowedTypes.contains(extension);
			}
		});

		DependencyResolutionResult result = projectDependenciesResolver.resolve(request);

		List<Dependency> dependencies = result.getResolvedDependencies();

		return dependencies;
	}
}
