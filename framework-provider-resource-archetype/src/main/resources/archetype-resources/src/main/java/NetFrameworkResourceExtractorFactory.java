package ${package}

import com.github.cs.NetFrameworkProvider;
import com.github.cs.NetFrameworkProviderFactory;

import java.io.File;
import java.util.Map;

/**
 * This {@link NetFrameworkProviderFactory} creates {@link NetFrameworkProvider framework providers} that extract the
 * framework as resource from the classpath. The directory to extract the framework to can be given as the property
 * "netFrameworkDirectory".
 *
 * @author miracelwhipp
 */
public class NetFrameworkResourceExtractorFactory implements NetFrameworkProviderFactory {

	public static String PROPERTY_EXTRACTION_TARGET_PATH = "netFrameworkDirectory";

	@Override
	public NetFrameworkResourceExtractor newFrameworkProvider(Map<String, String> configuration) {

		String extractionDirectory = configuration.get(PROPERTY_EXTRACTION_TARGET_PATH);

		if (extractionDirectory == null) {

			extractionDirectory = "target/net";
		}

		return new NetFrameworkResourceExtractor(new File(extractionDirectory));
	}
}
