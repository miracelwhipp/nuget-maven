package com.github.cs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * This class interfaces the command line to invoke the c# compiler.
 *
 * @author miracelwhipp
 */
public class CSharpCompiler {

	private static final List<String> DEFAULT_FRAMEWORK_REFERENCES = Arrays.asList("mscorlib", "System", "System.Xml",
			"Microsoft.CSharp", "System.Core", "System.Xml.Linq", "System.Data.DataSetExtensions", "System.Data");

	public static final String EXTENSION_DLL = ".dll";
	private final File workingDirectory;
	private final List<File> csSourceDirectories;
	private final List<File> referenceFiles;
	private final CSharpCompilerTargetType targetType;
	private final String targetFileName;
	private final String platform;
	private final List<String> defines;
	private final Log logger;
	private final NetFrameworkProvider frameworkProvider;
	private final List<String> frameworkReferences;
	private final List<String> resources;
	private final File keyFile;
	private final boolean unsafe;

	public CSharpCompiler(
			Log logger,
			File workingDirectory,
			List<File> csSourceDirectories,
			List<File> referenceFiles,
			CSharpCompilerTargetType targetType,
			String targetFileName,
			String platform,
			List<String> defines,
			NetFrameworkProvider frameworkProvider,
			List<String> frameworkReferences,
			List<String> resources,
			File keyFile,
			boolean unsafe
	) {
		this.logger = logger;
		this.workingDirectory = workingDirectory;
		this.csSourceDirectories = csSourceDirectories;
		this.referenceFiles = referenceFiles;
		this.targetType = targetType;
		this.targetFileName = targetFileName;
		this.platform = platform;
		this.defines = defines;
		this.frameworkProvider = frameworkProvider;
		this.frameworkReferences = frameworkReferences;
		this.resources = resources;
		this.keyFile = keyFile;
		this.unsafe = unsafe;
	}

	public File compile() throws MojoFailureException {

		try {

			File compilerExecutable = frameworkProvider.getCSharpCompiler();

			ProcessBuilder processBuilder = new ProcessBuilder(compilerExecutable.getPath());

			processBuilder.command().add("/nostdlib");
			processBuilder.command().add("/noconfig");
			processBuilder.command().add("/utf8output");

			if (unsafe) {
				processBuilder.command().add("/unsafe");
			}

			if (platform != null) {
				processBuilder.command().add("/platform:" + platform);
			}

			for (String define : defines) {
				processBuilder.command().add("/define:" + define);
			}

			processBuilder.command().add("/target:" + targetType.getArgumentId());

			String outFileName = targetFileName + "." + targetType.getFileSuffix();

			File targetFile = new File(workingDirectory, outFileName).getAbsoluteFile();

			processBuilder.command().add("/out:" + outFileName);

			for (File csSourceDirectory : csSourceDirectories) {

				processBuilder.command().add("/recurse:" + csSourceDirectory + "\\*.cs");
			}

			File frameworkLibraryPath = frameworkProvider.getFrameworkLibraryPath();

			List<String> frameworkLibraries = new ArrayList<>(DEFAULT_FRAMEWORK_REFERENCES);

			if (frameworkReferences != null) {
				frameworkLibraries.addAll(frameworkReferences);
			}

			for (String frameworkLibrary : frameworkLibraries) {
				processBuilder.command().add("/reference:" +
						new File(frameworkLibraryPath, frameworkLibrary + EXTENSION_DLL).getAbsolutePath());
			}

			for (File referenceFile : referenceFiles) {

				if (referenceFile.getName().endsWith(".netmodule") || referenceFile.getName().endsWith(".obj")) {

					processBuilder.command().add("/addmodule:" + referenceFile.getAbsolutePath());

				} else {

					processBuilder.command().add("/reference:" + referenceFile.getAbsolutePath());
				}
			}

			for (String resource : resources) {
				processBuilder.command().add("/resource:" + resource);
			}

			if (keyFile != null) {

				processBuilder.command().add("/keyfile:" + keyFile.getAbsolutePath());
			}

			logger.debug("executing csc:");
			for (String arg : processBuilder.command()) {
				logger.debug(arg);
			}

			processBuilder.directory(workingDirectory);
			processBuilder.inheritIO();


			Process process = processBuilder.start();

			process.waitFor();

			int exitValue = process.exitValue();

			if (exitValue != 0) {

				throw new MojoFailureException("c# compiler finished with exit value " + exitValue);
			}


			return targetFile;

		} catch (IOException | InterruptedException e) {

			throw new MojoFailureException(e.getMessage(), e);
		}
	}
}
