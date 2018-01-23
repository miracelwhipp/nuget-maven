package com.github.cs.nuget;

import org.apache.maven.wagon.ResourceDoesNotExistException;

import java.util.Locale;
import java.util.StringJoiner;

/**
 * This bean holds the information needed to download a package from nuget.
 *
 * @author miracelwhipp
 */
public class NugetArtifact {

	private final String id;
	private final String version;
	private final String type;

	public NugetArtifact(String id, String version, String type) {
		this.id = id;
		this.version = version;
		this.type = type;
	}

	public String getId() {
		return id;
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
				"id='" + id + '\'' +
				", version='" + version + '\'' +
				", type='" + type + '\'' +
				'}';
	}

	public String resourceString() {

		if ("pom".equals(type)) {
			return id + "/" + version + "/" + id + ".nuspec";
		}

		return id + "/" + version + "/" + id + "." + version + ".nupkg";
	}

	public static NugetArtifact fromMavenResourceString(String resourceName) throws ResourceDoesNotExistException {

		String[] parts = resourceName.split("/");

		int artifactIdPosition = parts.length - 3;


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

		if (!groupId.toString().equals(artifactId)) {

			throw new ResourceDoesNotExistException("resource does not exist : " + resourceName);
		}

		return fromParts(parts);
	}

	public static NugetArtifact fromParts(String[] parts) {

		int artifactIdPosition = parts.length - 3;
		int versionPosition = parts.length - 2;
		int typePosition = parts.length - 1;

		String artifactId = parts[artifactIdPosition];

		String version = parts[versionPosition];

		int position = parts[typePosition].lastIndexOf(".");

		String type = parts[typePosition].substring(position + 1);

		String id = artifactId.toLowerCase(Locale.ENGLISH);

		return new NugetArtifact(id, version, type);
	}
}
