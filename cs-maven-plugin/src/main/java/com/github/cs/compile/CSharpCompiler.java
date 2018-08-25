package com.github.cs.compile;

import com.github.cs.CSharpCompilerProvider;
import com.github.cs.DependencyProvider;
import com.github.cs.NetFrameworkProvider;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class interfaces the command line to invoke the c# compiler.
 *
 * @author miracelwhipp
 */
public class CSharpCompiler {

	private final SourceFiles sourceFiles;
	private final CSharpCompilerOptions compilerOptions;
	private final AssemblyFileProperties assemblyFileProperties;

	private final Log logger;
	private final NetFrameworkProvider frameworkProvider;
	private final CSharpCompilerProvider compilerProvider;


	public CSharpCompiler(
			SourceFiles sourceFiles,
			CSharpCompilerOptions compilerOptions,
			AssemblyFileProperties assemblyFileProperties,
			Log logger,
			NetFrameworkProvider frameworkProvider,
			CSharpCompilerProvider compilerProvider
	) {
		this.sourceFiles = sourceFiles;
		this.compilerOptions = compilerOptions;
		this.assemblyFileProperties = assemblyFileProperties;
		this.logger = logger;
		this.frameworkProvider = frameworkProvider;
		this.compilerProvider = compilerProvider;
	}

	public File compile() throws MojoFailureException {

		try {

			File compilerExecutable = compilerProvider.getCSharpCompiler();

			ProcessBuilder processBuilder = new ProcessBuilder(compilerExecutable.getPath());

			processBuilder.command().add("/nostdlib");
			processBuilder.command().add("/noconfig");
			processBuilder.command().add("/utf8output");

			if (compilerOptions.isUnsafe()) {
				processBuilder.command().add("/unsafe");
			}

			for (String define : compilerOptions.getDefines()) {
				processBuilder.command().add("/define:" + define);
			}

			if (assemblyFileProperties.getPlatform() != null) {
				processBuilder.command().add("/platform:" + assemblyFileProperties.getPlatform());
			}

			processBuilder.command().add("/target:" + assemblyFileProperties.getTargetType().getArgumentId());

			String outFileName = assemblyFileProperties.getTargetFileName() + "." +
					assemblyFileProperties.getTargetType().getFileSuffix();

			File targetFile = new File(sourceFiles.getWorkingDirectory(), outFileName).getAbsoluteFile();

			processBuilder.command().add("/out:" + outFileName);

			boolean sourceFilesExistent = false;

			for (File csSourceDirectory : sourceFiles.getCsSourceDirectories()) {

				if (!csSourceDirectory.exists()) {

					continue;
				}

				Collection<File> files = FileUtils.listFiles(csSourceDirectory, new String[]{"cs"}, true);

				for (File file : files) {

					processBuilder.command().add(file.getAbsolutePath());
					sourceFilesExistent = true;
				}
			}

			if (!sourceFilesExistent) {

				return null;
			}

			List<String> frameworkLibraries = new ArrayList<>();

			if (sourceFiles.getFrameworkReferences() != null) {
				frameworkLibraries.addAll(sourceFiles.getFrameworkReferences());
			}

			for (String frameworkLibrary : frameworkLibraries) {

				File library = frameworkProvider.getFrameworkLibrary(frameworkLibrary);

				if (library == null) {

					throw new MojoFailureException("cannot find framework library " + frameworkLibrary);
				}

				library = DependencyProvider.provideFile(library, null, sourceFiles.getWorkingDirectory());

				processBuilder.command().add("/reference:" + library.getAbsolutePath());
			}

			for (File referenceFile : sourceFiles.getReferenceFiles()) {

				if (referenceFile.getName().endsWith(".netmodule") || referenceFile.getName().endsWith(".obj")) {

					processBuilder.command().add("/addmodule:" + referenceFile.getAbsolutePath());

				} else {

					processBuilder.command().add("/reference:" + referenceFile.getAbsolutePath());
				}
			}

			for (String resource : sourceFiles.getResources()) {
				processBuilder.command().add("/resource:" + resource);
			}

			if (sourceFiles.getKeyFile() != null) {

				processBuilder.command().add("/keyfile:" + sourceFiles.getKeyFile().getAbsolutePath());
			}

			logger.debug("executing csc:");
			for (String arg : processBuilder.command()) {
				logger.debug(arg);
			}

			processBuilder.directory(sourceFiles.getWorkingDirectory());
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
