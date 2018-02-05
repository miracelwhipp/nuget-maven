package com.github.cs.nuget;

import java.util.Locale;

/**
 * This bean holds the information needed to download a package from nuget.
 *
 * @author miracelwhipp
 */
public class NugetArtifact {

	private final String groupId;
	private final String artifactId;
	private final String version;
	private final String type;

	private NugetArtifact(String groupId, String artifactId, String version, String type) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.type = type;
	}


	public String getArtifactId() {
		return artifactId;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getVersion() {
		return version;
	}

	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		return "NugetArtifact{" +
				"groupId='" + groupId + '\'' +
				", artifactId='" + artifactId + '\'' +
				", version='" + version + '\'' +
				", type='" + type + '\'' +
				'}';
	}

	public boolean isNugetFile() {
		return type.equals("nupkg") || type.equals("nuspec");
	}

	public boolean isPom() {
		return type.equals("pom");
	}

	public String resourceString() {

		if (isPom() || type.equals("nuspec")) {
			return groupId + "/" + version + "/" + groupId + ".nuspec";
		}

		return groupId + "/" + version + "/" + groupId + "." + version + ".nupkg";
	}

	public String artifactName() {

		return artifactId + "." + type;
	}

	public static NugetArtifact newInstance(String groupId, String artifactId, String version, String type) {

		return new NugetArtifact(
				groupId.toLowerCase(Locale.ENGLISH),
				artifactId.toLowerCase(Locale.ENGLISH),
				version.toLowerCase(Locale.ENGLISH),
				type.toLowerCase(Locale.ENGLISH)
		);
	}

	public static NugetArtifact fromMavenResourceString(String resourceName) {

		String[] parts = resourceName.split("/");

		int artifactIdPosition = parts.length - 3;
		int versionPosition = parts.length - 2;
		int typePosition = parts.length - 1;

		StringBuilder groupId = new StringBuilder();
		boolean first = true;

		for (int index = 0; index < artifactIdPosition; index++) {

			if (first) {
				first = false;
			} else {
				groupId.append(".");
			}

			groupId.append(parts[index]);
		}

		String artifactId = parts[artifactIdPosition];

		String version = parts[versionPosition];

		int position = parts[typePosition].lastIndexOf(".");

		String type = parts[typePosition].substring(position + 1);

		return newInstance(groupId.toString(), artifactId, version, type);
	}
}
