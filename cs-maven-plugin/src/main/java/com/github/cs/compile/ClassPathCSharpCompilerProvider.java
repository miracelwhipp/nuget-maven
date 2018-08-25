package com.github.cs.compile;

import com.github.cs.CSharpCompilerProvider;
import com.github.cs.Streams;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;
import java.io.IOException;

/**
 * This {@link CSharpCompilerProvider} downloads the c# compiler by the maven dependency mechanism.
 *
 * @author miracelwhipp
 */
@Component(role = CSharpCompilerProvider.class, hint = "default", instantiationStrategy = "singleton")
public class ClassPathCSharpCompilerProvider implements CSharpCompilerProvider {

	private File csharpCompiler;

	@Override
	public File getCSharpCompiler() throws IOException {

		if (csharpCompiler == null) {

			csharpCompiler = Streams.getResourceFile(getClass(), "tools", "csc", "exe");
		}

		return csharpCompiler;
	}
}
