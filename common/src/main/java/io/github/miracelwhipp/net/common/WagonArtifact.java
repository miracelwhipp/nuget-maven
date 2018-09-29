package io.github.miracelwhipp.net.common;

import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;

/**
 * TODO: document me
 *
 * @author miracelwhipp
 */
public class WagonArtifact {

	private final String groupId;
	private final String artifactId;
	private final boolean isMetadata;
	private final String version;
	private final String classifier;
	private final String type;

	private WagonArtifact(String groupId, String artifactId, boolean isMetadata, String version, String classifier, String type) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.isMetadata = isMetadata;
		this.version = version;
		this.classifier = classifier;
		this.type = type;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getClassifier() {
		return classifier;
	}

	public String getType() {
		return type;
	}

	public boolean isMetadata() {
		return isMetadata;
	}

	public File getRepositorySubdirectory() {

		return new File(new File(new File(groupIdPath(groupId)), artifactId), version);
	}

	public File getArtifactFilename() {

		return new File(getRepositorySubdirectory(), artifactId + "-" + version + (classifier.isEmpty() ? "" : "-" + classifier) + "." + type);
	}

	private String groupIdPath(String groupId) {
		return groupId.replaceAll("\\.", Matcher.quoteReplacement(File.separator));
	}


	public static WagonArtifact newInstance(
			String groupId, String artifactId, String version, String classifier, String type) {

		return newInstance(groupId, artifactId, version, classifier, type, false);
	}

	public static WagonArtifact newInstance(
			String groupId,
			String artifactId,
			String version,
			String classifier,
			String type,
			boolean isMetadata
	) {
		return new WagonArtifact(
				groupId.toLowerCase(Locale.ENGLISH),
				artifactId.toLowerCase(Locale.ENGLISH),
				isMetadata, version.toLowerCase(Locale.ENGLISH),
				classifier.toLowerCase(Locale.ENGLISH),
				type.toLowerCase(Locale.ENGLISH)
		);
	}

	public static WagonArtifact fromWagonResourceString(String resourceName) {

		String[] parts = resourceName.split("/");

		int artifactIdPosition = parts.length - 3;
		int versionPosition = parts.length - 2;
		int typePosition = parts.length - 1;

		StringBuilder groupId = new StringBuilder(resourceName.length());
		boolean first = true;

		for (int index = 0; index < artifactIdPosition; index++) {

			if (first) {
				first = false;
			} else {
				groupId.append(".");
			}

			groupId.append(parts[index]);
		}

		boolean isMetaData = parts[typePosition].equalsIgnoreCase("maven-metadata.xml");

		if (isMetaData) {

			if (first) {
				first = false;
			} else {
				groupId.append(".");
			}

			groupId.append(parts[artifactIdPosition]);
		}

		int position = parts[typePosition].lastIndexOf(".");

		String type = parts[typePosition].substring(position + 1);

		String artifactId = isMetaData ? parts[versionPosition] : parts[artifactIdPosition];

		String version = isMetaData ? "" : parts[versionPosition];

		int startClassifier = artifactId.length() + version.length() + 2;

		String classifier = isMetaData | startClassifier > position ? "" : parts[typePosition].substring(startClassifier, position);

		return newInstance(groupId.toString(), artifactId, version, classifier, type, isMetaData);
	}

	@Override
	public String toString() {
		return "WagonArtifact{" +
				"groupId='" + groupId + '\'' +
				", artifactId='" + artifactId + '\'' +
				", version='" + version + '\'' +
				", classifier='" + classifier + '\'' +
				", type='" + type + '\'' +
				'}';
	}
}
