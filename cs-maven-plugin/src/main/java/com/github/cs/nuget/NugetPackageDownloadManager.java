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

/**
 * This component manages downloads of .nupgk files.
 *
 * @author miracelwhipp
 */
@Component(role = NugetPackageDownloadManager.class, instantiationStrategy = "singleton")
public class NugetPackageDownloadManager {

	private final Map<String, File> localResources = new HashMap<>();

	public synchronized void get(Wagon delegate, NugetArtifact artifact, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		try {

			String key = artifact.resourceString();

			if (key.endsWith(".nuspec")) {

				delegate.get(key, destination);

				return;
			}

			File file = localResources.get(key);

			if (file != null) {

				FileUtils.copyFile(file, destination);

				return;
			}

			file = packageForArtifact(artifact, destination);

			delegate.get(key, file);

			localResources.put(key, file);

			FileUtils.copyFile(file, destination);

		} catch (IOException e) {

			throw new TransferFailedException(e.getMessage(), e);
		}
	}

	private File packageForArtifact(NugetArtifact artifact, File destination) {

		String groupIdPath = artifact.getGroupId().replaceAll("\\.", "\\" + File.separator);

		String destinationString = destination.getAbsolutePath().toLowerCase(Locale.ENGLISH);

		int groupIdPosition = destinationString.lastIndexOf(groupIdPath);

		if (groupIdPath.equals(artifact.getArtifactId())) {

			groupIdPosition = destinationString.lastIndexOf(groupIdPath, groupIdPosition - 1);
		}

		File repositoryDirectory = new File(destinationString.substring(0, groupIdPosition));

		return new File(
				new File(
						new File(
								new File(
										repositoryDirectory,
										groupIdPath
								),
								artifact.getGroupId()
						),
						artifact.getVersion()
				),
				artifact.getGroupId() + "-" + artifact.getVersion() + ".nupkg"
		);
	}

	public synchronized boolean getIfNewer(Wagon delegate, NugetArtifact artifact, File destination, long timestamp) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		try {

			String key = artifact.resourceString();

			if (key.endsWith(".nuspec")) {

				return delegate.getIfNewer(key, destination, timestamp);
			}

			File file = localResources.get(key);

			if (file != null) {

				if (Files.getLastModifiedTime(file.toPath()).toMillis() > timestamp) {

					FileUtils.copyFile(file, destination);

					return true;
				}

				return false;
			}

			file = packageForArtifact(artifact, destination);

			delegate.get(key, file);

			localResources.put(key, file);

			if (Files.getLastModifiedTime(file.toPath()).toMillis() > timestamp) {

				FileUtils.copyFile(file, destination);

				return true;
			}

			return false;

		} catch (IOException e) {

			throw new TransferFailedException(e.getMessage(), e);
		}
	}
}
