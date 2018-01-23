package com.github.cs;

import java.io.File;
import java.util.*;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.MavenProjectHelper;

/**
 * This goal compiles the c# sources and creates the projects artifact.
 *
 * @author miracelwhipp
 */
@Mojo(
		name = "compile",
		defaultPhase = LifecyclePhase.COMPILE,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class CompileCSharpMojo extends AbstractCompileCSharpMojo {

	/**
	 * This parameter specifies where the main sources are located.
	 */
	@Parameter(defaultValue = "${project.basedir}/src/main/cs", property = "cs.source.directory")
	private File csSourceDirectory;

	/**
	 * This parameter specifies additional directories where sources are located.
	 */
	@Parameter
	private List<String> additionalSourceDirectories;

	/**
	 * This parameter specifies files to be added as resources.
	 */
	@Parameter
	private List<String> resources = new ArrayList<>();

	@Parameter
	private List<String> preprocessorDefines = new ArrayList<>();

	@Parameter
	private String executableType;

	@Component
	private MavenProjectHelper projectHelper;

	private static final Set<String> ALLOWED_SCOPES =
			Collections.unmodifiableSet(new HashSet<>(Arrays.asList("compile", "provided", "system")));

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {

			CSharpCompilerTargetType targetType = getTargetType();

			if (targetType == null) {

				throw new MojoFailureException("unknown target type : " + project.getArtifact().getArtifactHandler().getPackaging());
			}

			if (targetType == CSharpCompilerTargetType.EXE) {

				if (CSharpCompilerTargetType.APP_CONTAINER.getArgumentId().equals(executableType)) {

					targetType = CSharpCompilerTargetType.APP_CONTAINER;

				} else if (CSharpCompilerTargetType.WINDOWS_EXE.getArgumentId().equals(executableType)) {

					targetType = CSharpCompilerTargetType.WINDOWS_EXE;
				}
			}

			String outputFile = getOutputFile();

			File assembly = compile(
					workingDirectory,
					csSourceDirectory,
					getGeneratedSourceDirectory(),
					additionalSourceDirectories,
					outputFile,
					targetType,
					ALLOWED_SCOPES,
					preprocessorDefines,
					resources
			);

			if (assembly == null) {

				throw new MojoFailureException("There were no main sources to compile.");
			}


			if (classifier == null) {

				project.getArtifact().setFile(assembly);

			} else {

				projectHelper.attachArtifact(project, targetType.getFileSuffix(), classifier, assembly);
			}


		} catch (DependencyResolutionException e) {

			throw new MojoFailureException(e.getMessage(), e);
		}
	}
}
