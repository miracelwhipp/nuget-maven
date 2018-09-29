package io.github.miracelwhipp.net.nuget.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * This class recreates the result of a nuget request to the index.json page of a nuget artifact.
 *
 * @author miracelwhipp
 */
public class NugetMetadata {

	List<String> versions = new ArrayList<>();

	public List<String> getVersions() {
		return versions;
	}

	public void setVersions(List<String> versions) {

		if (versions == null) {

			this.versions.clear();
			return;
		}

		this.versions = versions;
	}
}
