package com.github.cs.nuget;

import com.github.cs.FrameworkVersion;
import com.github.cs.NetFrameworkProvider;

import java.io.File;

/**
 * TODO: document me
 *
 * @author miracelwhipp
 */
class BootStrapNetFrameworkProvider implements NetFrameworkProvider {
	@Override
	public File getFrameworkLibrary(String name) {
		return null;
	}

	@Override
	public FrameworkVersion getFrameworkVersion() {
		return FrameworkVersion.defaultVersion();
	}
}
