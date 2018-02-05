package com.github.cs;

import java.io.File;

/**
 * Implement this interface to provide a mechanism to create instances of your {@link NetTestRunner}.
 *
 * @author miracelwhipp
 */
public interface NetTestRunnerFactory {

	/**
	 * This method returns a new {@link NetTestRunner} capable of running in the given directory.
	 *
	 * @param workingDirectory a directory containing the test dlls and their dependencies
	 * @return a new {@link NetTestRunner} capable of running in the given directory
	 */
	NetTestRunner newRunnerForDirectory(File workingDirectory);
}
