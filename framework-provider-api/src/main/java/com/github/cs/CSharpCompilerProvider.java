package com.github.cs;

import java.io.File;
import java.io.IOException;

/**
 * This interface provides methods to locate a c# compiler.
 *
 * @author miracelwhipp
 */
public interface CSharpCompilerProvider {

	/**
	 * This method returns the location of the c# compiler.
	 *
	 * @return the location of the c# compiler
	 */
	File getCSharpCompiler() throws IOException;


}
