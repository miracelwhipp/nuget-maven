package io.github.miracelwhipp.net.nuget.plugin;

import io.github.miracelwhipp.net.provider.FrameworkVersion;
import io.github.miracelwhipp.net.provider.NetFrameworkProvider;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * This wagon allows downloading dlls deployed in nuget as maven artifacts. It is implemented by simply
 * parsing and adapting the resource to load and delegating to {@link HttpWagon}. It can be used by specifying
 * nuget as protocol in the url. e.g. nuget://api.nuget.org/v3-flatcontainer/
 *
 * @author miracelwhipp
 */
@Component(role = Wagon.class, hint = "nuget", instantiationStrategy = "singleton")
public class NuGetWagon extends AbstractNugetWagon {

	@Requirement(hint = "https")
	private Wagon delegate;

	@Requirement
	private NugetPackageDownloadManager downloadManager;

	@Requirement
	private Logger logger;

	@Requirement
	private MavenSession session;

	@Override
	protected Wagon getDelegate() {
		return delegate;
	}

	@Override
	protected NugetPackageDownloadManager getDownloadManager() {
		return downloadManager;
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	protected FrameworkVersion getDefaultFrameworkVersion() {

		String property = session.getCurrentProject().getProperties().getProperty("net.framework.version", "netstandard2.0.3");

		return FrameworkVersion.fromShortName(property);
	}
}
