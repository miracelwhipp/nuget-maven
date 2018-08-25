package com.github.cs.xunit;

import com.github.cs.NetTestRunner;
import com.github.cs.NetTestRunnerFactory;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.io.File;

/**
 * TODO: document me
 *
 * @author miracelwhipp
 */
@Component(role = NetTestRunnerFactory.class, hint = "xunit")
public class XunitTestRunnerFactory implements NetTestRunnerFactory {

	@Requirement
	private Logger logger;


	@Override
	public NetTestRunner newRunnerForDirectory(File workingDirectory) {
		return new XUnitTestRunner(workingDirectory, logger);
	}
}
