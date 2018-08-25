package com.github.cs.compile;

import java.io.File;
import java.util.List;

/**
 * This immutable holds files that will compile to or be added otherwise to a c# assembly.
 *
 * @author miracelwhipp
 */
public class SourceFiles {

	private final File workingDirectory;
	private final List<File> csSourceDirectories;
	private final List<File> referenceFiles;
	private final List<String> frameworkReferences;
	private final List<String> resources;
	private final File keyFile;


	public SourceFiles(File workingDirectory, List<File> csSourceDirectories, List<File> referenceFiles, List<String> frameworkReferences, List<String> resources, File keyFile) {
		this.workingDirectory = workingDirectory;
		this.csSourceDirectories = csSourceDirectories;
		this.referenceFiles = referenceFiles;
		this.frameworkReferences = frameworkReferences;
		this.resources = resources;
		this.keyFile = keyFile;
	}

	public File getWorkingDirectory() {
		return workingDirectory;
	}

	public List<File> getCsSourceDirectories() {
		return csSourceDirectories;
	}

	public List<File> getReferenceFiles() {
		return referenceFiles;
	}

	public List<String> getFrameworkReferences() {
		return frameworkReferences;
	}

	public List<String> getResources() {
		return resources;
	}

	public File getKeyFile() {
		return keyFile;
	}
}
