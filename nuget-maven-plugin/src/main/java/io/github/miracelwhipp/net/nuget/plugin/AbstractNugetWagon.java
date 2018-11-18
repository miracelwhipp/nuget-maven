package io.github.miracelwhipp.net.nuget.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.miracelwhipp.net.common.Streams;
import io.github.miracelwhipp.net.common.Xml;
import io.github.miracelwhipp.net.provider.FrameworkVersion;
import io.github.miracelwhipp.net.provider.NetFrameworkProvider;
import org.apache.commons.io.FileUtils;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.logging.Logger;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

/**
 * This abstract class collects functionality to use nuget to download maven artifacts.
 *
 * @author miracelwhipp
 */
public abstract class AbstractNugetWagon implements Wagon {

	public static final String LIBRARY_DIRECTORY = "lib/";
	public static final String TOOLS_DIRECTORY = "tools/";
	public static final String REFERENCES_DIRECTORY = "ref/";
	public static final String SUFFIX_MD5 = ".md5";
	public static final String BUILD_DIRECTORY = "build/";

	public abstract Wagon getDelegate();

	public abstract NugetPackageDownloadManager getDownloadManager();

	public abstract NetFrameworkProvider getFrameworkProvider();

	public abstract Logger getLogger();

	@Override
	public synchronized void get(String resourceName, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		getLogger().debug("nuget download for " + resourceName + " to " + destination.getAbsolutePath());

		if (resourceName.endsWith(".sha1")) {

			getLogger().debug("resource is sha1 hash. not supported.");
			throw new ResourceDoesNotExistException("sha1 not supported.");
		}

		if (resourceName.endsWith(SUFFIX_MD5)) {

			getLogger().debug("resource is md5 hash.");
			getDownloadManager().getMd5Hash(getRepository(), NugetArtifact.fromMavenResourceString(resourceName.substring(0, resourceName.length() - SUFFIX_MD5.length())), destination);
			return;
		}

		NugetArtifact nugetArtifact = NugetArtifact.fromMavenResourceString(resourceName);

		if (nugetArtifact.isNugetFile()) {

			getLogger().debug("resource is nuget specific file.");
			getDownloadManager().getNugetFile(getDelegate(), nugetArtifact, destination);
			return;
		}

		if (nugetArtifact.isMetadata()) {

			getLogger().debug("resource is meta data only.");

			File jsonFile = new File(destination.getAbsolutePath() + ".json");
			getDownloadManager().getNugetFile(getDelegate(), nugetArtifact, jsonFile);

			transformResult(jsonFile, nugetArtifact, destination);
			return;
		}

		NugetArtifact downloadArtifact = nugetArtifact.correspondingDownloadArtifact();

		getLogger().debug("corresponding artifact is " + downloadArtifact);

		File downloadPackageFile = downloadPackageFile(downloadArtifact, nugetArtifact, destination);

		getDownloadManager().get(getDelegate(), downloadArtifact, downloadPackageFile);

		transformResult(downloadPackageFile, nugetArtifact, destination);
	}

	@Override
	public synchronized boolean getIfNewer(String resourceName, File destination, long timestamp) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		getLogger().debug("nuget download for " + resourceName + " to " + destination.getAbsolutePath());

		if (resourceName.endsWith(".md5") || resourceName.endsWith(".sha1")) {

			throw new ResourceDoesNotExistException("checksums not supported yet");
		}

		NugetArtifact nugetArtifact = NugetArtifact.fromMavenResourceString(resourceName);

		if (nugetArtifact.isNugetFile()) {

			getLogger().debug("resource is nuget specific file.");

			return getDownloadManager().getIfNewer(getDelegate(), nugetArtifact, destination, timestamp);
		}

		if (nugetArtifact.isMetadata()) {

			getLogger().debug("resource is meta data only.");

			File jsonFile = new File(destination.getAbsolutePath() + ".json");

			boolean result = getDownloadManager().getIfNewer(getDelegate(), nugetArtifact, jsonFile, timestamp);

			if (!result) {

				return false;
			}

			transformResult(jsonFile, nugetArtifact, destination);

			return true;
		}


		NugetArtifact downloadArtifact = nugetArtifact.correspondingDownloadArtifact();

		getLogger().debug("corresponding artifact is " + downloadArtifact);

		File downloadPackageFile = downloadPackageFile(downloadArtifact, nugetArtifact, destination);

		boolean result = getDownloadManager().getIfNewer(getDelegate(), downloadArtifact, downloadPackageFile, timestamp);

		if (!result) {

			return false;
		}

		transformResult(downloadPackageFile, nugetArtifact, destination);

		return true;
	}

	private File downloadPackageFile(NugetArtifact downloadArtifact, NugetArtifact artifact, File destination) {

		String groupIdPath = getGroupIdPath(artifact.getGroupId());

		String artifactPath = groupIdPath + File.separator + artifact.getArtifactId() + File.separator + artifact.getVersion();

		String destinationString = destination.getAbsolutePath();
//		String destinationString = destination.getAbsolutePath().toLowerCase(Locale.ENGLISH);

		int artifactPosition = destinationString.lastIndexOf(artifactPath);

		File repositoryDirectory = new File(destinationString.substring(0, artifactPosition));

		File result = repositoryDirectory.toPath().resolve(
				downloadArtifact.getWagonArtifact().getArtifactFilename().toPath()).toFile();

		getLogger().debug("download package file is " + result.getAbsolutePath());

		return result;
	}

	private String getGroupIdPath(String groupId) {
		return groupId.replaceAll("\\.", Matcher.quoteReplacement(File.separator));
	}

	@Override
	public boolean resourceExists(String resourceName) throws TransferFailedException, AuthorizationException {

		return getDelegate().resourceExists(NugetArtifact.fromMavenResourceString(resourceName).resourceString());
	}

	@Override
	public List<String> getFileList(String destinationDirectory) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		return getDelegate().getFileList(destinationDirectory);
	}

	private void transformResult(File downloadPackageFile, NugetArtifact nugetArtifact, File destination) throws TransferFailedException, ResourceDoesNotExistException {

		getLogger().debug("transforming result. downloadPackageFile : " + downloadPackageFile.getAbsolutePath() + " nuget artifact : " + nugetArtifact + " destination " + destination.getAbsolutePath());

		if (nugetArtifact.isNugetFile()) {

			getLogger().debug("artifact is nuget specific file. Nothing to be done.");
			return;
		}

		if (nugetArtifact.isMetadata()) {

			getLogger().debug("transforming to meta data xml.");

			transFormToMetaDataXml(nugetArtifact, downloadPackageFile, destination);

			return;
		}

		if (nugetArtifact.isPom()) {

			getLogger().debug("transforming to pom.");

			transFormToPom(downloadPackageFile, destination);

			return;
		}

		if (nugetArtifact.getType().equals("dll")) {

			getLogger().debug("extracting library.");

			extractLibrary(downloadPackageFile, nugetArtifact, destination);

			return;
		}

		if (nugetArtifact.getType().equals("exe")) {

			getLogger().debug("extracting tool.");

			extractTool(downloadPackageFile, nugetArtifact, destination);

			return;
		}

		getLogger().debug("nothing to be done.");
	}

	private void transFormToMetaDataXml(NugetArtifact nugetArtifact, File jsonFile, File destination) throws TransferFailedException {

		try {
			ObjectMapper mapper = new ObjectMapper();

			NugetMetadata nugetMetadata = mapper.readValue(jsonFile, NugetMetadata.class);

			StringBuilder builder = new StringBuilder();

			builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
					"<metadata modelVersion=\"1.1.0\">\n" +
					"  <groupId>");

			builder.append(nugetArtifact.getGroupId());

			builder.append("</groupId>\n" +
					"  <artifactId>");

			builder.append(nugetArtifact.getArtifactId());

			builder.append("</artifactId>\n" +
					"  <versioning>\n");

			if (!nugetMetadata.getVersions().isEmpty()) {
				builder.append("    <latest>");
				builder.append(nugetMetadata.getVersions().get(nugetMetadata.getVersions().size() - 1));
				builder.append("</latest>\n");
			}

			String releaseVersion = findReleaseVersion(nugetMetadata);

			if (releaseVersion != null) {

				builder.append("    <release>");
				builder.append(releaseVersion);
				builder.append("</release>\n");
			}

			builder.append("    <versions>\n");

			for (String version : nugetMetadata.getVersions()) {

				builder.append("      <version>");
				builder.append(version);
				builder.append("</version>\n");
			}


			builder.append("    </versions>\n" +
//					"    <lastUpdated>20171020062041</lastUpdated>\n" +
					"  </versioning>\n" +
					"</metadata>\n");

			FileUtils.write(destination, builder.toString(), StandardCharsets.UTF_8);

		} catch (IOException e) {

			throw new TransferFailedException(e.getMessage(), e);
		}

	}

	private String findReleaseVersion(NugetMetadata nugetMetadata) {

		for (int index = nugetMetadata.getVersions().size() - 1; index >= 0; index--) {

			String version = nugetMetadata.getVersions().get(index);

			if (version.matches("[0-9]+(\\.[0-9]+)*")) {

				return version;
			}

		}

		return null;
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

		getLogger().debug("desired version is " + desiredVersion.toString());

		try {

			getLogger().debug("unpacking " + downloadPackageFile.getAbsolutePath() + " in order to get file " + nugetArtifact.specificationFile().getAbsolutePath());
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

		if (result != null) {

			return result;
		}

		File buildDirectory = new File(directory, BUILD_DIRECTORY);

		result = new File(new File(new File(buildDirectory,
				desiredVersion.versionedToken()), REFERENCES_DIRECTORY), nugetArtifact.artifactName());

		if (result.exists()) {

			return result;
		}

		result = searchDirectoryForVersionedFile(nugetArtifact, desiredVersion, buildDirectory);

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

			destination.getParentFile().mkdirs();

			Files.createLink(destination.toPath(), file.toPath());

		} else {

			throw new ResourceDoesNotExistException("no compatible artifact found in nuget package");
		}
	}

	private void transFormToPom(File sourceFile, File destination) throws TransferFailedException {

		try (InputStream source = AbstractNugetWagon.class.getResourceAsStream("/nuspec-to-pom.xsl")) {

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
		return getDelegate().getRepository();
	}

	@Override
	public void connect(Repository source) throws ConnectionException, AuthenticationException {

		adaptRepository(source);

		getDelegate().connect(source);
	}

	private void adaptRepository(Repository source) {

		source.setUrl(source.getUrl().replaceFirst("nuget://", "https://"));
	}

	@Override
	public void connect(Repository source, ProxyInfo proxyInfo) throws ConnectionException, AuthenticationException {

		adaptRepository(source);

		getDelegate().connect(source, proxyInfo);
	}

	@Override
	public void connect(Repository source, ProxyInfoProvider proxyInfoProvider) throws
			ConnectionException, AuthenticationException {

		adaptRepository(source);

		getDelegate().connect(source, proxyInfoProvider);
	}

	@Override
	public void connect(Repository source, AuthenticationInfo authenticationInfo) throws
			ConnectionException, AuthenticationException {

		adaptRepository(source);

		getDelegate().connect(source, authenticationInfo);
	}

	@Override
	public void connect(Repository source, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo) throws
			ConnectionException, AuthenticationException {

		adaptRepository(source);

		getDelegate().connect(source, authenticationInfo);
	}

	@Override
	public void connect(Repository source, AuthenticationInfo authenticationInfo, ProxyInfoProvider
			proxyInfoProvider) throws ConnectionException, AuthenticationException {

		adaptRepository(source);

		getDelegate().connect(source, authenticationInfo, proxyInfoProvider);
	}

	@Override
	public void openConnection() throws ConnectionException, AuthenticationException {
		getDelegate().openConnection();
	}

	@Override
	public void disconnect() throws ConnectionException {
		getDelegate().disconnect();
	}

	@Override
	public void setTimeout(int timeoutValue) {
		getDelegate().setTimeout(timeoutValue);
	}

	@Override
	public int getTimeout() {
		return getDelegate().getTimeout();
	}

	@Override
	public void setReadTimeout(int timeoutValue) {
		getDelegate().setReadTimeout(timeoutValue);
	}

	@Override
	public int getReadTimeout() {
		return getDelegate().getReadTimeout();
	}

	@Override
	public void addSessionListener(SessionListener listener) {
		getDelegate().addSessionListener(listener);
	}

	@Override
	public void removeSessionListener(SessionListener listener) {
		getDelegate().removeSessionListener(listener);
	}

	@Override
	public boolean hasSessionListener(SessionListener listener) {
		return getDelegate().hasSessionListener(listener);
	}

	@Override
	public void addTransferListener(TransferListener listener) {
		getDelegate().addTransferListener(listener);
	}

	@Override
	public void removeTransferListener(TransferListener listener) {
		getDelegate().removeTransferListener(listener);
	}

	@Override
	public boolean hasTransferListener(TransferListener listener) {
		return getDelegate().hasTransferListener(listener);
	}

	@Override
	public boolean isInteractive() {
		return getDelegate().isInteractive();
	}

	@Override
	public void setInteractive(boolean interactive) {
		getDelegate().setInteractive(interactive);
	}
}
