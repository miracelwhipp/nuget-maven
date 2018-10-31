package io.github.miracelwhipp.net.nuget.plugin;

import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This component manages downloads of .nupgk files.
 *
 * @author miracelwhipp
 */
@Component(role = NugetPackageDownloadManager.class, instantiationStrategy = "singleton")
public class NugetPackageDownloadManager {

	@Requirement
	private Logger logger;

	private final Map<String, File> localResources = new HashMap<>();

	public synchronized void get(Wagon delegate, NugetArtifact artifact, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		logger.debug("downloadmanager.get " + artifact.toString() + " to " + destination.getAbsolutePath());

		if (destination.exists()) {

			logger.debug("File already exists. Nothing to be done.");

			return;
		}

		String key = artifact.resourceString();

		logger.debug("Acquiring lock for " + key + "...");
		final File lock = getLock(key, destination);

		synchronized (lock) {

			logger.debug("Lock acquired for " + key + "...");

			if (destination.exists()) {

				logger.debug("File was created while waiting for lock. Nothing to be done.");
				return;
			}

			File tempFile = new File(destination.getAbsolutePath() + ".tmp" + UUID.randomUUID().toString());

			logger.debug("downloading temp-file " + tempFile.getAbsolutePath());
			delegate.get(key, tempFile);

			if (!tempFile.renameTo(destination)) {

				throw new TransferFailedException("cannot rename file. source = " + tempFile.getAbsolutePath() + " target = " + destination.getAbsolutePath());
			}

			logger.debug("Download done. tempfile renamed to " + destination.getAbsolutePath());
		}
	}

	private synchronized File getLock(String key, File destination) {

		File result = localResources.get(key);

		if (result != null) {

			return result;
		}

		localResources.put(key, destination);

		return destination;
	}

	public synchronized boolean getIfNewer(Wagon delegate, NugetArtifact artifact, File destination, long timestamp) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		logger.debug("downloadmanager.getIfNewer " + artifact.toString() + " to " + destination.getAbsolutePath() + " for timestamp " + timestamp);

		if (destination.exists() && destination.lastModified() > timestamp) {

			logger.debug("File already exists and is last modified " + destination.lastModified() + ". Nothing to be done.");

			return true;
		}

		String key = artifact.resourceString();

		logger.debug("Acquiring lock for " + key + "...");
		final File lock = getLock(key, destination);

		synchronized (lock) {

			logger.debug("Lock acquired for " + key + "...");
			if (destination.exists() && destination.lastModified() > timestamp) {

				logger.debug("File was modified at " + destination.lastModified() + " while waiting for lock. Nothing to be done.");
				return true;
			}

			File tempFile = new File(destination.getAbsolutePath() + ".tmp" + UUID.randomUUID().toString());

			logger.debug("downloading temp-file " + tempFile.getAbsolutePath());
			boolean result = delegate.getIfNewer(key, tempFile, timestamp);

			if (!result) {

				logger.debug("Delegate omitted download.");
				return false;
			}

			if (!tempFile.renameTo(destination)) {

				throw new TransferFailedException("cannot rename file. source = " + tempFile.getAbsolutePath() + " target = " + destination.getAbsolutePath());
			}

			logger.debug("Download done. tempfile renamed to " + destination.getAbsolutePath());

			return true;
		}
	}

	public void getNugetFile(Wagon delegate, NugetArtifact artifact, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		logger.debug("downloadmanager.getNugetFile " + artifact.toString() + " to " + destination.getAbsolutePath());

		File wrappedDestination = new File(destination.getAbsolutePath() + ".working");

		if (wrappedDestination.exists()) {

			logger.debug("File already exists. Nothing to be done.");

			return;
		}

		String key = artifact.resourceString();

		logger.debug("Acquiring lock for " + key + "...");
		final File lock = getLock(key, wrappedDestination);

		synchronized (lock) {

			logger.debug("Lock acquired for " + key + "...");

			if (wrappedDestination.exists()) {

				logger.debug("File was created while waiting for lock. Nothing to be done.");
				return;
			}

			try {

				File parentDirectory = wrappedDestination.getParentFile();

				if (!parentDirectory.exists()) {

					parentDirectory.mkdirs();
				}

				wrappedDestination.createNewFile();

			} catch (IOException e) {

				throw new TransferFailedException(e.getMessage(), e);
			}

			delegate.get(key, destination);
		}
	}

	public void getMd5Hash(Repository repository, NugetArtifact artifact, File destination) throws TransferFailedException, ResourceDoesNotExistException {

		logger.debug("downloadmanager.getMd5Hash " + artifact.toString() + " to " + destination.getAbsolutePath());

		if (!artifact.isNugetFile()) {

			throw new ResourceDoesNotExistException("hash for files extracted from nuget archive not supported.");
		}

		try (CloseableHttpClient client = HttpClients.createDefault()) {

			HttpHead headRequest = new HttpHead(repository.getUrl() + "/" + artifact.resourceString());

			logger.debug("invoking HTTP HEAD " + repository.getUrl() + "/" + artifact.resourceString());

			try (CloseableHttpResponse response = client.execute(headRequest)) {

				Header checkSum = response.getFirstHeader("Content-MD5");

				if (checkSum == null) {

					throw new ResourceDoesNotExistException("Content-MD5 header not set");
				}

				FileUtils.write(destination, checkSum.getValue(), StandardCharsets.ISO_8859_1);
			}

		} catch (IOException e) {

			throw new TransferFailedException(e.getMessage(), e);
		}

	}
}
