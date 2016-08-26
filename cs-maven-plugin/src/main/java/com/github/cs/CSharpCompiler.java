package com.github.cs;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * This class interfaces the command line to invoke the c# compiler.
 *
 * @author miracelwhipp
 */
public class CSharpCompiler {

	private final File workingDirectory;
	private final File csSourceDirectory;
	private final List<File> referenceFiles;
	private final CSharpCompilerTargetType targetType;
	private final String targetFileName;
	private final List<String> defines;
	private final Log logger;
	private final NetFrameworkProvider frameworkProvider;

	public CSharpCompiler(
			Log logger,
			File workingDirectory,
			File csSourceDirectory,
			List<File> referenceFiles,
			CSharpCompilerTargetType targetType,
			String targetFileName,
			List<String> defines,
			NetFrameworkProvider frameworkProvider
	) {
		this.logger = logger;
		this.workingDirectory = workingDirectory;
		this.csSourceDirectory = csSourceDirectory;
		this.referenceFiles = referenceFiles;
		this.targetType = targetType;
		this.targetFileName = targetFileName;
		this.defines = defines;
		this.frameworkProvider = frameworkProvider;
	}

	public File compile() throws MojoFailureException {

		try {

			File compilerExecutable = frameworkProvider.getCSharpCompiler();

			ProcessBuilder processBuilder = new ProcessBuilder(compilerExecutable.getPath());

			processBuilder.command().add("/nostdlib");
			processBuilder.command().add("/noconfig");
			processBuilder.command().add("/utf8output");

			processBuilder.command().add("/target:" + targetType.getArgumentId());

			String outFileName = targetFileName + "." + targetType.getFileSuffix();

			File targetFile = new File(workingDirectory, outFileName).getAbsoluteFile();

			processBuilder.command().add("/out:" + outFileName);

			processBuilder.command().add("/recurse:" + csSourceDirectory + "\\*.cs");


			File[] frameworkLibraries = frameworkProvider.getFrameworkLibraryPath().listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {

					String name = pathname.getName();

					if (!name.endsWith(".dll")) {

						return false;
					}

					if (name.startsWith("mscorlib.dll")) {

						return true;
					}

					if (name.startsWith("System.EnterpriseServices.")) {

						return false;
					}

					return name.startsWith("System.");
				}
			});

			for (File frameworkLibrary : frameworkLibraries) {
				processBuilder.command().add("/reference:" + frameworkLibrary.getAbsolutePath());
			}

			for (File referenceFile : referenceFiles) {

				processBuilder.command().add("/reference:" + referenceFile.getAbsolutePath());
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
