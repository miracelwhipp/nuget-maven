package com.github.cs;

import java.io.File;
import java.io.IOException;

/**
 * This interface provides methods to locate a c# compiler and an according ms core library.
 *
 * @author miracelwhipp
 */
public interface NetFrameworkProvider {

	/**
	 * This method returns the location of the c# compiler.
	 *
	 * @return the location of the c# compiler
	 */
	File getCSharpCompiler() throws IOException;

	/**
	 * This method returns the directory where the .Net framework libraries are located.
	 *
	 * @return the location of the ms core library
	 */
	File getFrameworkLibraryPath() throws IOException;


	/**
	 * This method returns the location of the nunit runner executable.
	 *
	 * @return the location of the nunit runner executable
	 */
	File getNUnitRunner() throws IOException;

	/**
	 * This method returns the location of the nunit core library.
	 *
	 * @return the location of the nunit core library
	 * @throws IOException
	 */
	File getNUnitLibrary() throws IOException;

}
