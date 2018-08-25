package com.github.cs.nuget;

import com.github.cs.FrameworkVersion;
import com.github.cs.NetFrameworkProvider;
import com.github.cs.Streams;
import com.github.cs.Xml;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

/**
 * This wagon allows downloading dlls deployed in nuget as maven artifacts. It is implemented by simply
 * parsing and adapting the resource to load and delegating to {@link HttpWagon}. It can be used by specifying
 * nuget as protocol in the url. e.g. nuget://api.nuget.org/v3-flatcontainer/
 *
 * @author miracelwhipp
 */
@Component(role = Wagon.class, hint = "nuget", instantiationStrategy = "singleton")
public class NuGetWagon implements Wagon {

	public static final String LIBRARY_DIRECTORY = "lib/";
	public static final String TOOLS_DIRECTORY = "tools/";
	public static final String REFERENCES_DIRECTORY = "ref/";
	public static final String SUFFIX_MD5 = ".md5";

	@Requirement(optional = true)
	private NetFrameworkProvider frameworkProvider;

	@Requirement(hint = "https")
	private Wagon delegate;

	@Requirement
	private NugetPackageDownloadManager downloadManager;

	@Override
	public synchronized void get(String resourceName, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		if (resourceName.endsWith(".sha1")) {

			throw new ResourceDoesNotExistException("sha1 not supported.");
		}

		if (resourceName.endsWith(SUFFIX_MD5)) {

			downloadManager.getMd5Hash(getRepository(), NugetArtifact.fromMavenResourceString(resourceName.substring(0, resourceName.length() - SUFFIX_MD5.length())), destination);
			return;
		}

		NugetArtifact nugetArtifact = NugetArtifact.fromMavenResourceString(resourceName);

		if (nugetArtifact.isNugetFile()) {

			downloadManager.getNugetFile(delegate, nugetArtifact, destination);
			return;
		}

		NugetArtifact downloadArtifact = nugetArtifact.correspondingDownloadArtifact();

		File downloadPackageFile = downloadPackageFile(downloadArtifact, nugetArtifact, destination);

		downloadManager.get(delegate, downloadArtifact, downloadPackageFile);

		transformResult(downloadPackageFile, nugetArtifact, destination);
	}

	@Override
	public synchronized boolean getIfNewer(String resourceName, File destination, long timestamp) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		if (resourceName.endsWith(".md5") || resourceName.endsWith(".sha1")) {

			throw new ResourceDoesNotExistException("checksums not supported yet");
		}

		NugetArtifact nugetArtifact = NugetArtifact.fromMavenResourceString(resourceName);

		if (nugetArtifact.isNugetFile()) {

			return downloadManager.getIfNewer(delegate, nugetArtifact, destination, timestamp);
		}


		NugetArtifact downloadArtifact = nugetArtifact.correspondingDownloadArtifact();

		File downloadPackageFile = downloadPackageFile(downloadArtifact, nugetArtifact, destination);

		boolean result = downloadManager.getIfNewer(delegate, downloadArtifact, downloadPackageFile, timestamp);

		if (!result) {

			return false;
		}

		transformResult(downloadPackageFile, nugetArtifact, destination);

		return true;
	}

	private File downloadPackageFile(NugetArtifact downloadArtifact, NugetArtifact artifact, File destination) {

		String groupIdPath = getGroupIdPath(artifact.getGroupId());

		String artifactPath = groupIdPath + File.separator + artifact.getArtifactId() + File.separator + artifact.getVersion();

		String destinationString = destination.getAbsolutePath().toLowerCase(Locale.ENGLISH);

		int artifactPosition = destinationString.lastIndexOf(artifactPath);

		File repositoryDirectory = new File(destinationString.substring(0, artifactPosition));

		return repositoryDirectory.toPath().resolve(
				downloadArtifact.getWagonArtifact().getArtifactFilename().toPath()).toFile();
	}

	private String getGroupIdPath(String groupId) {
		return groupId.replaceAll("\\.", Matcher.quoteReplacement(File.separator));
	}


	@Override
	public boolean resourceExists(String resourceName) throws TransferFailedException, AuthorizationException {

		return delegate.resourceExists(NugetArtifact.fromMavenResourceString(resourceName).resourceString());
	}

	@Override
	public List<String> getFileList(String destinationDirectory) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		return delegate.getFileList(destinationDirectory);
	}

	private NetFrameworkProvider getFrameworkProvider() {

		if (frameworkProvider == null) {

			frameworkProvider = new BootStrapNetFrameworkProvider();
		}

		return frameworkProvider;
	}

	private void transformResult(File downloadPackageFile, NugetArtifact nugetArtifact, File destination) throws TransferFailedException, ResourceDoesNotExistException {

		if (nugetArtifact.isNugetFile()) {

			return;
		}

		if (nugetArtifact.isPom()) {

			transFormToPom(downloadPackageFile, destination);

			return;
		}

		if (nugetArtifact.getType().equals("dll")) {

			extractLibrary(downloadPackageFile, nugetArtifact, destination);

			return;
		}

		if (nugetArtifact.getType().equals("exe")) {

			extractTool(downloadPackageFile, nugetArtifact, destination);
		}
	}

	private void extractTool(File downloadPackageFile, NugetArtifact nugetArtifact, File destination) throws TransferFailedException, ResourceDoesNotExistException {

		try {

			File file = Streams.unpackForFile(downloadPackageFile, new File(TOOLS_DIRECTORY, nugetArtifact.artifactName()));

			provideUnpackedFile(destination, file);

		} catch (IOException e) {

			throw new TransferFailedException(e.getMessage(), e);
		}
	}

	private void extractLibrary(File downloadPackageFile, NugetArtifact nugetArtifact, File destination)
			throws TransferFailedException, ResourceDoesNotExistException {

		FrameworkVersion desiredVersion = getFrameworkProvider().getFrameworkVersion();

		if (desiredVersion == null) {

			desiredVersion = FrameworkVersion.defaultVersion();
		}

		try {

			File specification = Streams.unpackForFile(downloadPackageFile, nugetArtifact.specificationFile());

			File file = findLibrary(nugetArtifact, desiredVersion, specification, specification.getParentFile());

			provideUnpackedFile(destination, file);

		} catch (IOException e) {

			throw new TransferFailedException(e.getMessage(), e);
		}
	}

	private File findLibrary(final NugetArtifact nugetArtifact, final FrameworkVersion desiredVersion,
	                         File specification, File directory) throws ResourceDoesNotExistException {

		File libraryDirectory = new File(directory, LIBRARY_DIRECTORY);

		File result = new File(
				new File(libraryDirectory, desiredVersion.versionedToken()), nugetArtifact.artifactName());

		if (result.exists()) {

			return result;
		}

		result = searchDirectoryForVersionedFile(nugetArtifact, desiredVersion, libraryDirectory);

		if (result == null) {

			result = new File(new File(directory, TOOLS_DIRECTORY), nugetArtifact.artifactName());
		}

		if (result.exists()) {

			return result;
		}

		result = searchDirectoryForVersionedFile(
				nugetArtifact, desiredVersion, new File(directory, REFERENCES_DIRECTORY));

		if (result == null) {

			throw new ResourceDoesNotExistException("no compatible artifact found in nuget package");
		}

		return result;
	}

	private File searchDirectoryForVersionedFile(
			final NugetArtifact nugetArtifact, FrameworkVersion desiredVersion, File directory) {

		File[] directories = directory.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {

				if (!pathname.isDirectory()) {

					return false;
				}

				if (!new File(pathname, nugetArtifact.artifactName()).exists()) {

					return false;
				}

				return true;
			}
		});

		if (directories == null) {

			return null;
		}

		FrameworkVersion extractedVersion = null;

		for (File pathname : directories) {

			FrameworkVersion currentVersion = FrameworkVersion.fromShortName(pathname.getName());

			if (currentVersion == null) {

				continue;
			}

			if (!desiredVersion.isDownwardsCompatible(currentVersion)) {

				continue;
			}

			if (extractedVersion != null && !currentVersion.isDownwardsCompatible(extractedVersion)) {

				continue;
			}

			extractedVersion = currentVersion;
		}

		if (extractedVersion == null) {

			return null;
		}

		return new File(
				new File(directory, extractedVersion.versionedToken()), nugetArtifact.artifactName());
	}

	private void provideUnpackedFile(File destination, File file) throws TransferFailedException, IOException, ResourceDoesNotExistException {

		if (file.exists()) {

			if (destination.exists() && !destination.delete()) {

				throw new TransferFailedException("cannot delete file " + destination.getAbsolutePath());
			}

			Files.createLink(destination.toPath(), file.toPath());

		} else {

			throw new ResourceDoesNotExistException("no compatible artifact found in nuget package");
		}
	}

	private void transFormToPom(File sourceFile, File destination) throws TransferFailedException {

		try (InputStream source = NuGetWagon.class.getResourceAsStream("/nuspec-to-pom.xsl")) {

			Xml.transformFile(sourceFile, new StreamSource(source), destination, false, new TargetFrameworkParameterSetter(getFrameworkProvider().getFrameworkVersion().versionedFullName()));

		} catch (IOException | ParserConfigurationException | TransformerException | SAXException e) {

			throw new TransferFailedException(e.getMessage(), e);
		}
	}

	@Override
	public void put(File source, String destination) throws
			TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
		throw new UnsupportedOperationException("nuget wagon is read-only");
	}

	@Override
	public void putDirectory(File sourceDirectory, String destinationDirectory) throws
			TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
		throw new UnsupportedOperationException("nuget wagon is read-only");
	}

	@Override
	public boolean supportsDirectoryCopy() {
		return false;
	}

	@Override
	public Repository getRepository() {
		return delegate.getRepository();
	}

	@Override
	public void connect(Repository source) throws ConnectionException, AuthenticationException {

		adaptRepository(source);

		delegate.connect(source);
	}

	private void adaptRepository(Repository source) {

		source.setUrl(source.getUrl().replaceFirst("nuget://", "https://"));
	}

	@Override
	public void connect(Repository source, ProxyInfo proxyInfo) throws ConnectionException, AuthenticationException {

		adaptRepository(source);

		delegate.connect(source, proxyInfo);
	}

	@Override
	public void connect(Repository source, ProxyInfoProvider proxyInfoProvider) throws
			ConnectionException, AuthenticationException {

		adaptRepository(source);

		delegate.connect(source, proxyInfoProvider);
	}

	@Override
	public void connect(Repository source, AuthenticationInfo authenticationInfo) throws
			ConnectionException, AuthenticationException {

		adaptRepository(source);

		delegate.connect(source, authenticationInfo);
	}

	@Override
	public void connect(Repository source, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo) throws
			ConnectionException, AuthenticationException {

		adaptRepository(source);

		delegate.connect(source, authenticationInfo);
	}

	@Override
	public void connect(Repository source, AuthenticationInfo authenticationInfo, ProxyInfoProvider
			proxyInfoProvider) throws ConnectionException, AuthenticationException {

		adaptRepository(source);

		delegate.connect(source, authenticationInfo, proxyInfoProvider);
	}

	@Override
	public void openConnection() throws ConnectionException, AuthenticationException {
		delegate.openConnection();
	}

	@Override
	public void disconnect() throws ConnectionException {
		delegate.disconnect();
	}

	@Override
	public void setTimeout(int timeoutValue) {
		delegate.setTimeout(timeoutValue);
	}

	@Override
	public int getTimeout() {
		return delegate.getTimeout();
	}

	@Override
	public void setReadTimeout(int timeoutValue) {
		delegate.setReadTimeout(timeoutValue);
	}

	@Override
	public int getReadTimeout() {
		return delegate.getReadTimeout();
	}

	@Override
	public void addSessionListener(SessionListener listener) {
		delegate.addSessionListener(listener);
	}

	@Override
	public void removeSessionListener(SessionListener listener) {
		delegate.removeSessionListener(listener);
	}

	@Override
	public boolean hasSessionListener(SessionListener listener) {
		return delegate.hasSessionListener(listener);
	}

	@Override
	public void addTransferListener(TransferListener listener) {
		delegate.addTransferListener(listener);
	}

	@Override
	public void removeTransferListener(TransferListener listener) {
		delegate.removeTransferListener(listener);
	}

	@Override
	public boolean hasTransferListener(TransferListener listener) {
		return delegate.hasTransferListener(listener);
	}

	@Override
	public boolean isInteractive() {
		return delegate.isInteractive();
	}

	@Override
	public void setInteractive(boolean interactive) {
		delegate.setInteractive(interactive);
	}

}
