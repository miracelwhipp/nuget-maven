package com.github.cs;

import org.apache.maven.plugin.AbstractMojo;
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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * This abstract class works as base class for c#/.net maven goals. It collects common components and parameters.
 *
 * @author miracelwhipp
 */
public abstract class AbstractNetMojo extends AbstractMojo {

	@Parameter(defaultValue = "")
	protected Map<String, String> frameworkProviderConfiguration;

	@Parameter(readonly = true, defaultValue = "${project}")
	protected MavenProject project;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	private RepositorySystemSession repoSession;

	@Component
	private ProjectDependenciesResolver projectDependenciesResolver;

	private NetFrameworkProvider frameworkProvider;

	protected synchronized NetFrameworkProvider getFrameworkProvider() {

		if (frameworkProvider != null) {

			return frameworkProvider;
		}

		frameworkProvider = loadFrameworkProvider();

		return frameworkProvider;
	}

	private NetFrameworkProvider loadFrameworkProvider() {

		ServiceLoader<NetFrameworkProviderFactory> serviceLoader =
				ServiceLoader.load(NetFrameworkProviderFactory.class);

		Iterator<NetFrameworkProviderFactory> providerIterator = serviceLoader.iterator();

		Map<String, String> frameworkProviderConfiguration = this.frameworkProviderConfiguration;

		if (frameworkProviderConfiguration == null) {

			frameworkProviderConfiguration = Collections.emptyMap();
		}

		if (!providerIterator.hasNext()) {

			//fall back to default provider, that expects the compiler executable to be in the path environment variable
			return new DefaultNetFrameworkProvider(frameworkProviderConfiguration);
		}

		NetFrameworkProviderFactory result = providerIterator.next();

		if (providerIterator.hasNext()) {

			getLog().warn("several implementations for " + NetFrameworkProviderFactory.class.getCanonicalName() + " found. Using " + result.getClass().getCanonicalName());
		}

		return result.newFrameworkProvider(frameworkProviderConfiguration);
	}

	protected List<Dependency> getDllDependencies(final Set<String> allowedScopes) throws DependencyResolutionException {

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

				return extension.equals("dll");
			}
		});

		DependencyResolutionResult result = projectDependenciesResolver.resolve(request);

		List<Dependency> dllDependencies = result.getResolvedDependencies();

		return dllDependencies;
	}


}
