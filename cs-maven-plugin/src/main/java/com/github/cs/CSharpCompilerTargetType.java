package com.github.cs;

import java.util.EnumSet;

/**
 * This enumeration holds the different kinds of artifacts that can be build with the c# compiler.
 *
 * @author miracelwhipp
 */
public enum CSharpCompilerTargetType {

	APP_CONTAINER("appcontainerexe", "exe"),
	EXE("exe", "exe"),
	LIBRARY("library", "dll"),
	MODULE("module", "netmodule"),
	WINDOWS_EXE("winexe", "exe"),
	WIN_MD_OBJ("winmdobj", "winmdobj");

	private final String argumentId;
	private final String fileSuffix;

	CSharpCompilerTargetType(String argumentId, String fileSuffix) {
		this.argumentId = argumentId;
		this.fileSuffix = fileSuffix;
	}

	public String getArgumentId() {
		return argumentId;
	}

	public String getFileSuffix() {
		return fileSuffix;
	}

	public static CSharpCompilerTargetType fromString(String name) {

		for (CSharpCompilerTargetType member : EnumSet.allOf(CSharpCompilerTargetType.class)) {

			if (member.getArgumentId().equals(name)) {

				return member;
			}
		}

		for (CSharpCompilerTargetType member : EnumSet.allOf(CSharpCompilerTargetType.class)) {

			if (member.getFileSuffix().equals(name)) {

				return member;
			}
		}

		return null;
	}
}
