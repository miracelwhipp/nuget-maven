package io.github.miracelwhipp.net.nuget.plugin;

import io.github.miracelwhipp.net.provider.FrameworkVersion;
import io.github.miracelwhipp.net.provider.NetFrameworkProvider;

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
