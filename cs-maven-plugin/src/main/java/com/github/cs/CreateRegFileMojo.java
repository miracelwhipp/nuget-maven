package com.github.cs;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.io.IOException;

/**
 * This goal generates a .reg file for easy registration of the assemblies COM components, if the assembly is a COM dll.
 *
 * @author miracelwhipp
 */
@Mojo(
		name = "reg-asm",
		defaultPhase = LifecyclePhase.COMPILE,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class CreateRegFileMojo extends AbstractNetMojo {

	@Component
	private MavenProjectHelper projectHelper;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (getTargetType() != CSharpCompilerTargetType.LIBRARY) {

			return;
		}

		if (!comVisible) {

			return;
		}

		try {

			File regAsmDir = getFrameworkProvider().getCSharpCompiler().getParentFile();

			File regAsm = new File(regAsmDir, "RegAsm.exe");

			ProcessBuilder processBuilder = new ProcessBuilder(regAsm.getAbsolutePath());

			String output = project.getBuild().getDirectory() + "\\" + getOutputFile();

			String outputFile = output + ".reg";

			processBuilder.command().add("/regfile:" + outputFile);
			processBuilder.command().add(output + ".dll");

			processBuilder.directory(workingDirectory);
			processBuilder.inheritIO();

			Process process = processBuilder.start();

			process.waitFor();

			int exitValue = process.exitValue();

			if (exitValue != 0) {

				throw new MojoFailureException("regasm.exe finished with exit value " + exitValue);
			}

			projectHelper.attachArtifact(project, "reg", classifier, new File(outputFile));

		} catch (IOException | InterruptedException e) {

			throw new MojoFailureException(e.getMessage(), e);
		}

	}
}
