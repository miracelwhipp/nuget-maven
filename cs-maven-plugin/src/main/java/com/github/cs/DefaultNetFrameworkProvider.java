package com.github.cs;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * This {@link NetFrameworkProvider} works as fallback and assumes the c# compiler csc.exe to be in the path
 * environment variable.
 *
 * @author miracelwhipp
 */
public class DefaultNetFrameworkProvider implements NetFrameworkProvider {

	private final Map<String, String> configuration;

	public DefaultNetFrameworkProvider(Map<String, String> configuration) {
		this.configuration = configuration;
	}


	@Override
	public File getCSharpCompiler() {

		String csCompiler = configuration.get("csCompiler");

		if (csCompiler != null) {

			return new File(csCompiler.trim());
		}

		return new File("csc.exe");
	}

	@Override
	public File getFrameworkLibraryPath() {

		String coreLib = configuration.get("frameworkLibraryPath");

		if (coreLib != null) {

			return new File(coreLib.trim());
		}

		return new File(".");
	}

	@Override
	public File getNUnitRunner() throws IOException {

		String nUnitRunner = configuration.get("nUnitRunner");

		if (nUnitRunner != null) {

			return new File(nUnitRunner.trim());
		}

		return new File("nunit3-console.exe");
	}

	@Override
	public File getNUnitLibrary() throws IOException {

		String nUnitLibrary = configuration.get("nUnitLibrary");

		if (nUnitLibrary != null) {

			return new File(nUnitLibrary.trim());
		}

		return new File("nunit.framework.dll");
	}
}
