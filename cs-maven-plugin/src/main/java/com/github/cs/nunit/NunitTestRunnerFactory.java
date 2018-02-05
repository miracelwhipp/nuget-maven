package com.github.cs.nunit;

import com.github.cs.NetTestRunner;
import com.github.cs.NetTestRunnerFactory;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.io.File;

/**
 * This {@link NetTestRunnerFactory} provides nunit3 as test runner
 *
 * @author miracelwhipp
 */
@Component(role = NetTestRunnerFactory.class, hint = "nunit")
public class NunitTestRunnerFactory implements NetTestRunnerFactory {

	@Requirement
	private Logger logger;

	@Override
	public NunitTestRunner newRunnerForDirectory(File workingDirectory) {

		return new NunitTestRunner(workingDirectory, logger);
	}
}
