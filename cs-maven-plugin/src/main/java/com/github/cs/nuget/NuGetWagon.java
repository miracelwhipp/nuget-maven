package com.github.cs.nuget;

import com.github.cs.FrameworkVersion;
import com.github.cs.NetFrameworkProvider;
import com.github.cs.XmlUtility;
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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

	@Requirement
	private NetFrameworkProvider frameworkProvider;

	@Requirement(hint = "https")
	private Wagon delegate;

	@Requirement
	private NugetPackageDownloadManager downloadManager;

	@Override
	public void get(String resourceName, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		if (resourceName.endsWith(".md5") || resourceName.endsWith(".sha1")) {

			throw new ResourceDoesNotExistException("checksums not supported yet");
		}

		NugetArtifact nugetArtifact = NugetArtifact.fromMavenResourceString(resourceName);

		downloadManager.get(delegate, nugetArtifact, destination);

		transformResult(destination, nugetArtifact);
	}

	@Override
	public boolean getIfNewer(String resourceName, File destination, long timestamp) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		if (resourceName.endsWith(".md5") || resourceName.endsWith(".sha1")) {

			throw new ResourceDoesNotExistException("checksums not supported yet");
		}

		NugetArtifact nugetArtifact = NugetArtifact.fromMavenResourceString(resourceName);

		boolean result = downloadManager.getIfNewer(delegate, nugetArtifact, destination, timestamp);

		if (!result) {

			return false;
		}

		transformResult(destination, nugetArtifact);

		return true;
	}

	@Override
	public boolean resourceExists(String resourceName) throws TransferFailedException, AuthorizationException {

		return delegate.resourceExists(NugetArtifact.fromMavenResourceString(resourceName).resourceString());
	}

	@Override
	public List<String> getFileList(String destinationDirectory) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		return delegate.getFileList(destinationDirectory);
	}

	private void transformResult(File destination, NugetArtifact nugetArtifact) throws TransferFailedException, ResourceDoesNotExistException {

		if (nugetArtifact.isNugetFile()) {

			return;
		}

		if (nugetArtifact.isPom()) {

			transFormToPom(destination);

			return;

		}

		if (nugetArtifact.getType().equals("dll")) {

			extractLibrary(destination, nugetArtifact);

			return;
		}

		if (nugetArtifact.getType().equals("exe")) {

			extractTool(destination, nugetArtifact);

		}
	}

	private void extractTool(File destination, NugetArtifact nugetArtifact) throws TransferFailedException, ResourceDoesNotExistException {

		File tempFile = new File(destination.getPath() + ".temp");

		try (ZipInputStream source = new ZipInputStream(new FileInputStream(destination))) {

			for (ZipEntry entry = source.getNextEntry(); entry != null; entry = source.getNextEntry()) {

				String entryName = entry.getName().toLowerCase(Locale.ENGLISH);

				if (!entryName.startsWith(TOOLS_DIRECTORY)) {

					continue;
				}

				entryName = entryName.substring(TOOLS_DIRECTORY.length());

				if (!entryName.equals(nugetArtifact.artifactName())) {

					continue;
				}

				try (FileOutputStream target = new FileOutputStream(tempFile)) {
					byte[] buffer = new byte[128 * 1024];

					int bytesRead = 0;

					while ((bytesRead = source.read(buffer)) > 0) {

						target.write(buffer, 0, bytesRead);
					}
				}

				break;
			}

		} catch (IOException e) {

			throw new TransferFailedException(e.getMessage(), e);
		}

		if (tempFile.exists()) {

			if (!destination.delete()) {

				throw new TransferFailedException("cannot delete file " + destination.getAbsolutePath());
			}

			if (!tempFile.renameTo(destination)) {

				throw new TransferFailedException("cannot rename file " + tempFile.getAbsolutePath() + " to " + destination.getAbsolutePath());
			}

		} else {

			throw new ResourceDoesNotExistException("no compatible artifact found in nuget package");
		}

	}

	private void extractLibrary(File destination, NugetArtifact nugetArtifact) throws TransferFailedException, ResourceDoesNotExistException {

		FrameworkVersion desiredVersion = frameworkProvider.getFrameworkVersion();

		if (desiredVersion == null) {

			desiredVersion = FrameworkVersion.newInstance(4, 7, 1);
		}

		File tempFile = new File(destination.getPath() + ".temp");

		try (ZipInputStream source = new ZipInputStream(new FileInputStream(destination))) {

			FrameworkVersion extractedVersion = null;

			for (ZipEntry entry = source.getNextEntry(); entry != null; entry = source.getNextEntry()) {

				String entryName = entry.getName().toLowerCase(Locale.ENGLISH);

				if (!entryName.startsWith(LIBRARY_DIRECTORY)) {

					continue;
				}

				entryName = entryName.substring(LIBRARY_DIRECTORY.length());

				int position = entryName.indexOf("/");

				if (position == -1) {

					continue;
				}

				String artifactName = entryName.substring(position + 1);

				if (!artifactName.equals(nugetArtifact.artifactName())) {

					continue;
				}

				FrameworkVersion currentVersion = FrameworkVersion.fromShortName(entryName.substring(0, position));

				if (currentVersion == null) {

					continue;
				}

				if (!desiredVersion.isDownwardsCompatible(currentVersion)) {

					continue;
				}

				if (extractedVersion != null && !currentVersion.isDownwardsCompatible(extractedVersion)) {

					continue;
				}

				try (FileOutputStream target = new FileOutputStream(tempFile)) {
					byte[] buffer = new byte[128 * 1024];

					int bytesRead = 0;

					while ((bytesRead = source.read(buffer)) > 0) {

						target.write(buffer, 0, bytesRead);
					}
				}

				extractedVersion = currentVersion;
				source.closeEntry();

				if (currentVersion.equals(desiredVersion)) {

					break;
				}
			}

		} catch (IOException e) {

			throw new TransferFailedException(e.getMessage(), e);
		}

		if (!tempFile.exists()) {

			//maybe a tool provides this dll
			extractTool(destination, nugetArtifact);

		} else {

			if (!destination.delete()) {

				throw new TransferFailedException("cannot delete file " + destination.getAbsolutePath());
			}

			if (!tempFile.renameTo(destination)) {

				throw new TransferFailedException("cannot rename file " + tempFile.getAbsolutePath() + " to " + destination.getAbsolutePath());
			}
		}
	}

	private void transFormToPom(File destination) throws TransferFailedException {
		try (InputStream source = NuGetWagon.class.getResourceAsStream("/nuspec-to-pom.xsl")) {

			XmlUtility.transformFile(destination, new StreamSource(source), destination, new XmlUtility.ParameterSetter() {
				@Override
				public void setParameters(Transformer transformer) {
					transformer.setParameter("targetFramework", frameworkProvider.getFrameworkVersion().versionedFullName());
				}
			});

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
