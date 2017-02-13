package com.github.cs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 *
 * This goal generates an AssemblyInfo.cs source file with assembly information.
 *
 * @author jschwarz
 */
@Mojo(
		name = "generate-assembly-info",
		defaultPhase = LifecyclePhase.GENERATE_SOURCES,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class GenerateAssemblyInfo extends AbstractMojo {

	private static final String FILE_NAME = "AssemblyInfo.cs";
	public static final int BUFFER_SIZE = 512 * 1024;
	public static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

	@Parameter(defaultValue = "false", property = "cs.assembly.info.skip")
	private boolean skip;

	@Parameter(defaultValue = "${project.build.directory}/generated-sources/main/cs", property = "cs.generated.source.directory")
	private File generatedSourceDirectory;

	@Parameter(defaultValue = "${project.name}", property = "cs.assembly.title")
	private String title;

	@Parameter(defaultValue = "${project.description}", property = "cs.assembly.description")
	private String description;

	@Parameter(property = "cs.assembly.configuration")
	private String configuration;

	@Parameter(property = "cs.assembly.company")
	private String company;

	@Parameter(defaultValue = "${project.name}", property = "cs.assembly.product")
	private String product;

	@Parameter(property = "cs.assembly.copyright")
	private String copyright;

	@Parameter(property = "cs.assembly.trademark")
	private String trademark;

	@Parameter(property = "cs.assembly.culture")
	private String culture;

	@Parameter(defaultValue = "false", property = "cs.assembly.com.visible")
	private boolean comVisible;

	@Parameter(property = "cs.assembly.guid")
	private String guid;

	@Parameter(defaultValue = "${project.version}", property = "cs.assembly.version")
	private String version;

	@Parameter(defaultValue = "${project.version}", property = "cs.assembly.file.version")
	private String fileVersion;

	@Parameter(readonly = true, defaultValue = "${project}")
	protected MavenProject project;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (skip) {

			return;
		}

		if (title == null) {

			title = project.getArtifactId();
		}

		if (product == null) {

			product = project.getArtifactId();
		}

		if (guid == null) {

			guid = UUID.nameUUIDFromBytes((
					project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion()
			).getBytes(StandardCharsets.UTF_8)).toString();
		}

		if (version.endsWith(SNAPSHOT_SUFFIX)) {

			version = version.substring(0, version.length() - SNAPSHOT_SUFFIX.length());
		}

		if (fileVersion.endsWith(SNAPSHOT_SUFFIX)) {

			fileVersion = fileVersion.substring(0, fileVersion.length() - SNAPSHOT_SUFFIX.length());
		}

		if (copyright == null && company != null) {

			copyright = "Copyright © " + company;
		}

		if (copyright == null) {

			copyright = "";
		}

		if (company == null) {

			company = "";
		}

		if (culture == null) {

			culture = "";
		}

		if (trademark == null) {

			trademark = "";
		}

		if (configuration == null) {

			configuration = "";
		}

		if (description == null) {

			description = "";
		}

		if (!generatedSourceDirectory.exists()) {

			if (!generatedSourceDirectory.mkdirs()) {

				throw new MojoExecutionException("unable to create directory " + generatedSourceDirectory);
			}

		} else {

			if (!generatedSourceDirectory.isDirectory()) {

				throw new MojoExecutionException("generated sources directory is a file");
			}
		}

		try (
				InputStream source = GenerateAssemblyInfo.class.getClassLoader().
						getResourceAsStream("assembly-info-template.cs");
				ByteArrayOutputStream memory = new ByteArrayOutputStream();
				OutputStream target = new FileOutputStream(new File(generatedSourceDirectory, FILE_NAME))
		) {

			int read;
			byte[] buffer = new byte[BUFFER_SIZE];

			while ((read = source.read(buffer)) >= 0) {

				memory.write(buffer, 0, read);
			}

			String assemblyInfo = new String(buffer, StandardCharsets.UTF_8);

			assemblyInfo = assemblyInfo.replace("${assembly.title}", title);
			assemblyInfo = assemblyInfo.replace("${assembly.description}", description);
			assemblyInfo = assemblyInfo.replace("${assembly.configuration}", configuration);
			assemblyInfo = assemblyInfo.replace("${assembly.company}", company);
			assemblyInfo = assemblyInfo.replace("${assembly.product}", product);
			assemblyInfo = assemblyInfo.replace("${assembly.copyright}", copyright);
			assemblyInfo = assemblyInfo.replace("${assembly.trademark}", trademark);
			assemblyInfo = assemblyInfo.replace("${assembly.culture}", culture);
			assemblyInfo = assemblyInfo.replace("${assembly.com.visible}", Boolean.toString(comVisible));
			assemblyInfo = assemblyInfo.replace("${assembly.guid}", guid);
			assemblyInfo = assemblyInfo.replace("${assembly.version}", version);
			assemblyInfo = assemblyInfo.replace("${assembly.file.version}", fileVersion);

			target.write(assemblyInfo.getBytes(StandardCharsets.UTF_8));

		} catch (IOException e) {

			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
