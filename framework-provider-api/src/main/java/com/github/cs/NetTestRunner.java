package com.github.cs;

import java.io.File;
import java.util.List;

/**
 * Implement this interface to provide an implementation for running .net tests.
 *
 * @author miracelwhipp
 */
public interface NetTestRunner {


	void runTests(File testLibrary, List<String> includes, List<String> excludes, File resultFile) throws TestExecutionException;
}
