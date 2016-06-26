package ${package}

import com.github.cs.NetFrameworkProvider;
import com.github.resource.bunch.ResourceBunchExtractor;

import java.io.File;
import java.io.IOException;

/**
 * This {@link NetFrameworkProvider} extracts a .net framework as
 * resource from the classpath.
 *
 * @author miracelwhipp
 */
public class NetFrameworkResourceExtractor implements NetFrameworkProvider {

	private final File extractionDirectory;

	private File frameworkDirectory = null;
	private File nUnitDirectory = null;

	public NetFrameworkResourceExtractor(File extractionDirectory) {
		this.extractionDirectory = extractionDirectory;
	}

	private synchronized void extract() throws IOException {

		if (frameworkDirectory != null) {

			return;
		}

		File markerFile = new File(extractionDirectory, "extraction.done");

		frameworkDirectory = new File(extractionDirectory, "framework/FRAMEWORK-SUBDIR");
		nUnitDirectory = new File(extractionDirectory, "n-unit");

		if (markerFile.exists()) {

			return;
		}

		ResourceBunchExtractor.extract("framework", extractionDirectory);
		ResourceBunchExtractor.extract("n-unit", extractionDirectory);

		markerFile.createNewFile();

	}

	@Override
	public File getCSharpCompiler() throws IOException {

		extract();

		return new File(frameworkDirectory, "csc.exe").getAbsoluteFile();
	}

	@Override
	public File getMsCoreLibrary() throws IOException {

		extract();

		return new File(frameworkDirectory, "mscorlib.dll").getAbsoluteFile();
	}

	@Override
	public File getNUnitRunner() throws IOException {

		extract();

		return new File(nUnitDirectory, "nunit-console/nunit3-console.exe").getAbsoluteFile();
	}

	@Override
	public File getNUnitLibrary() throws IOException {

		extract();

		return new File(nUnitDirectory, "framework/3.2.1.0/FRAMEWORK-VERSION/nunit.framework.dll").getAbsoluteFile();
	}
}
