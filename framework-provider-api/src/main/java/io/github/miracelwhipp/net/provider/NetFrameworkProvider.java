package io.github.miracelwhipp.net.provider;

import java.io.File;
import java.io.IOException;

/**
 * This interface provides methods to locate an ms core library.
 *
 * @author miracelwhipp
 */
public interface NetFrameworkProvider {

	/**
	 * This method returns the file name of a given frame work library.
	 *
	 * @param name the name of the framework library to get.
	 * @return the location of the .Net framework libraries
	 */
	File getFrameworkLibrary(String name) throws IOException;

	FrameworkVersion getFrameworkVersion();

}
