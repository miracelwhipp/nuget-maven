package com.github.cs.nuget;

import com.github.cs.XmlTransformation;
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
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
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
 * nuget as protocoll in the url. e.g. nuget://api.nuget.org/v3-flatcontainer/
 *
 * @author miracelwhipp
 */
public class NuGetWagon implements Wagon {

	private final HttpWagon delegate;

	public NuGetWagon() {
		this.delegate = new HttpWagon();
	}

	@Override
	public void get(String resourceName, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		NugetArtifact nugetArtifact = NugetArtifact.fromMavenResourceString(resourceName);

		delegate.get(nugetArtifact.resourceString(), destination);

		transformResult(destination, nugetArtifact);
	}

	@Override
	public boolean getIfNewer(String resourceName, File destination, long timestamp) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		NugetArtifact nugetArtifact = NugetArtifact.fromMavenResourceString(resourceName);

		boolean result = delegate.getIfNewer(nugetArtifact.resourceString(), destination, timestamp);

		if (!result) {

			return false;
		}

		transformResult(destination, nugetArtifact);

		return true;
	}

	@Override
	public boolean resourceExists(String resourceName) throws TransferFailedException, AuthorizationException {

		String[] parts = resourceName.split("/");

		String groupId = parts[0];

		String artifactId = parts[1];

		if (!groupId.equals(artifactId)) {

			return false;
		}

		return delegate.resourceExists(NugetArtifact.fromParts(parts).resourceString());
	}

	@Override
	public List<String> getFileList(String destinationDirectory) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		return delegate.getFileList(destinationDirectory);
	}

	private void transformResult(File destination, NugetArtifact nugetArtifact) throws TransferFailedException {

		if (nugetArtifact.getType().equals("pom")) {

			try (InputStream source = NuGetWagon.class.getResourceAsStream("/nuspec-to-pom.xsl")) {

				XmlTransformation.transformFile(destination, new StreamSource(source), destination);

			} catch (IOException | ParserConfigurationException | TransformerException | SAXException e) {

				throw new TransferFailedException(e.getMessage(), e);
			}

		} else if (nugetArtifact.getType().equals("dll") || nugetArtifact.getType().equals("exe")) {

			File tempFile = new File(destination.getPath() + ".temp");

			try (ZipInputStream source = new ZipInputStream(new FileInputStream(destination))) {

				for (ZipEntry entry = source.getNextEntry(); entry != null; entry = source.getNextEntry()) {

					if (entry.getName().toLowerCase(Locale.ENGLISH).endsWith(nugetArtifact.getId() + "." + nugetArtifact.getType())) {

						try (FileOutputStream target = new FileOutputStream(tempFile)) {
							byte[] buffer = new byte[128 * 1024];

							int bytesRead = 0;

							while ((bytesRead = source.read(buffer)) > 0) {

								target.write(buffer, 0, bytesRead);
							}
						}

						source.closeEntry();
						break;
					}

					source.closeEntry();
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
			}
		}
	}

	@Override
	public void put(File source, String destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
		throw new UnsupportedOperationException("nuget wagon is read-only");
	}

	@Override
	public void putDirectory(File sourceDirectory, String destinationDirectory) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
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
	public void connect(Repository source, ProxyInfoProvider proxyInfoProvider) throws ConnectionException, AuthenticationException {

		adaptRepository(source);

		delegate.connect(source, proxyInfoProvider);
	}

	@Override
	public void connect(Repository source, AuthenticationInfo authenticationInfo) throws ConnectionException, AuthenticationException {

		adaptRepository(source);

		delegate.connect(source, authenticationInfo);
	}

	@Override
	public void connect(Repository source, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo) throws ConnectionException, AuthenticationException {

		adaptRepository(source);

		delegate.connect(source, authenticationInfo);
	}

	@Override
	public void connect(Repository source, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider) throws ConnectionException, AuthenticationException {

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
