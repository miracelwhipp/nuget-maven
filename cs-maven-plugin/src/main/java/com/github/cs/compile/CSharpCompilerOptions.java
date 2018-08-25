package com.github.cs.compile;

import java.util.List;

/**
 * This class holds options to be given to a c# compiler.
 *
 * @author miracelwhipp
 */
public class CSharpCompilerOptions {

	private final List<String> defines;
	private final boolean unsafe;

	public CSharpCompilerOptions(List<String> defines, boolean unsafe) {
		this.defines = defines;
		this.unsafe = unsafe;
	}

	public List<String> getDefines() {
		return defines;
	}

	public boolean isUnsafe() {
		return unsafe;
	}
}
