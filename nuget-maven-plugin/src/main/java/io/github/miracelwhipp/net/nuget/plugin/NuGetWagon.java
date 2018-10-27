package io.github.miracelwhipp.net.nuget.plugin;

import io.github.miracelwhipp.net.provider.NetFrameworkProvider;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * This wagon allows downloading dlls deployed in nuget as maven artifacts. It is implemented by simply
 * parsing and adapting the resource to load and delegating to {@link HttpWagon}. It can be used by specifying
 * nuget as protocol in the url. e.g. nuget://api.nuget.org/v3-flatcontainer/
 *
 * @author miracelwhipp
 */
@Component(role = Wagon.class, hint = "nuget", instantiationStrategy = "singleton")
public class NuGetWagon extends AbstractNugetWagon {

	@Requirement(optional = true)
	private NetFrameworkProvider frameworkProvider;

	@Requirement(hint = "https")
	private Wagon delegate;

	@Requirement
	private NugetPackageDownloadManager downloadManager;

	@Override
	public Wagon getDelegate() {
		return delegate;
	}

	@Override
	public NugetPackageDownloadManager getDownloadManager() {
		return downloadManager;
	}

	@Override
	public NetFrameworkProvider getFrameworkProvider() {

		if (frameworkProvider == null) {

			frameworkProvider = new BootStrapNetFrameworkProvider();
		}

		return frameworkProvider;
	}


}
