package io.github.miracelwhipp.net.nuget.plugin;

import io.github.miracelwhipp.net.common.WagonArtifact;

import java.io.File;

/**
 * This bean holds the information needed to download a package from nuget.
 *
 * @author miracelwhipp
 */
public class NugetArtifact {

	public static final String EXTENSION_SPECIFICATION = ".nuspec";
	public static final String EXTENSION_PACKAGE = ".nupkg";
	public static final String TYPE_SPECIFICATION = "nuspec";
	public static final String TYPE_PACKAGE = "nupkg";


	private final WagonArtifact wagonArtifact;

	private NugetArtifact(WagonArtifact wagonArtifact) {
		this.wagonArtifact = wagonArtifact;
	}

	public String getArtifactId() {
		return wagonArtifact.getArtifactId();
	}

	public String getGroupId() {
		return wagonArtifact.getGroupId();
	}

	public String getVersion() {
		return wagonArtifact.getVersion();
	}

	public String getClassifier() {
		return wagonArtifact.getClassifier();
	}

	public String getType() {
		return wagonArtifact.getType();
	}

	public boolean isMetadata() {

		return wagonArtifact.isMetadata();
	}

	public WagonArtifact getWagonArtifact() {
		return wagonArtifact;
	}

	@Override
	public String toString() {
		return "NugetArtifact{" +
				"wagonArtifact=" + wagonArtifact +
				'}';
	}

	public boolean isNugetFile() {
		return getType().equals(TYPE_PACKAGE) || isNuSpec();
	}

	private boolean isNuSpec() {
		return getType().equals(TYPE_SPECIFICATION);
	}

	public boolean isPom() {
		return getType().equals("pom");
	}

	public boolean isSpec() {

		return isPom() || isNuSpec();
	}

	public String resourceString() {

		if (isMetadata()) {

			return getGroupId() + "/index.json";
		}

		if (isSpec()) {
			return getGroupId() + "/" + getVersion() + "/" + getGroupId() + EXTENSION_SPECIFICATION;
		}

		return getGroupId() + "/" + getVersion() + "/" + getGroupId() + "." + getVersion() + EXTENSION_PACKAGE;
	}

	public String artifactName() {

		return getArtifactId() + "." + getType();
	}

	public File specificationFile() {

		return new File(getGroupId() + EXTENSION_SPECIFICATION);
	}

	public NugetArtifact correspondingDownloadArtifact() {

		if (isSpec()) {

			return new NugetArtifact(WagonArtifact.newInstance(
					getGroupId(), getGroupId(), getVersion(), getClassifier(), TYPE_SPECIFICATION));
		}

		return new NugetArtifact(WagonArtifact.newInstance(
				getGroupId(), getGroupId(), getVersion(), getClassifier(), TYPE_PACKAGE));

	}

	public static NugetArtifact newInstance(
			String groupId, String artifactId, String version, String classifier, String type) {

		return new NugetArtifact(WagonArtifact.newInstance(groupId, artifactId, version, classifier, type));
	}

	public static NugetArtifact fromMavenResourceString(String resourceName) {

		return new NugetArtifact(WagonArtifact.fromWagonResourceString(resourceName));
	}
}
