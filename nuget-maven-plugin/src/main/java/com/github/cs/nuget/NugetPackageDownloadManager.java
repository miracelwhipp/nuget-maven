package com.github.cs.nuget;

import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * This component manages downloads of .nupgk files.
 *
 * @author miracelwhipp
 */
@Component(role = NugetPackageDownloadManager.class, instantiationStrategy = "singleton")
public class NugetPackageDownloadManager {

	private final Map<String, File> localResources = new HashMap<>();

	public synchronized void get(Wagon delegate, NugetArtifact artifact, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		if (destination.exists()) {

			return;
		}

		String key = artifact.resourceString();

		final File lock = getLock(key, destination);

		synchronized (lock) {

			if (destination.exists()) {

				return;
			}

			File tempFile = new File(destination.getAbsolutePath() + ".tmp" + UUID.randomUUID().toString());

			delegate.get(key, tempFile);

			if (!tempFile.renameTo(destination)) {

				throw new TransferFailedException("cannot rename file. source = " + tempFile.getAbsolutePath() + " target = " + destination.getAbsolutePath());
			}
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

		if (destination.exists() && destination.lastModified() > timestamp) {

			return true;
		}

		String key = artifact.resourceString();

		final File lock = getLock(key, destination);

		synchronized (lock) {

			if (destination.exists() && destination.lastModified() > timestamp) {

				return true;
			}

			File tempFile = new File(destination.getAbsolutePath() + ".tmp" + UUID.randomUUID().toString());

			boolean result = delegate.getIfNewer(key, tempFile, timestamp);

			if (!result) {

				return false;
			}

			if (!tempFile.renameTo(destination)) {

				throw new TransferFailedException("cannot rename file. source = " + tempFile.getAbsolutePath() + " target = " + destination.getAbsolutePath());
			}

			return true;
		}
	}

	public void getNugetFile(Wagon delegate, NugetArtifact artifact, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		File wrappedDestination = new File(destination.getAbsolutePath() + ".working");

		if (wrappedDestination.exists()) {

			return;
		}

		String key = artifact.resourceString();

		final File lock = getLock(key, wrappedDestination);

		synchronized (lock) {

			if (wrappedDestination.exists()) {

				return;
			}

			try {

				wrappedDestination.createNewFile();

			} catch (IOException e) {

				throw new TransferFailedException(e.getMessage(), e);
			}

			delegate.get(key, destination);
		}
	}
}
