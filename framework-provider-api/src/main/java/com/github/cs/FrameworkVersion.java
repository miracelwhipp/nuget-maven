package com.github.cs;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class holds data about the version of a .Net framework
 *
 * @author miracelwhipp
 */
public class FrameworkVersion {

	private static final Map<String, String> FULL_NAME_BY_SHORT_NAME = new HashMap<>();

	private static final Pattern VERSION_PATTERN =
			Pattern.compile("(?<abbreviation>[a-zA-Z]*)" +
					"(?:(?<longMajor>[0-9]+)\\.|(?<shortMajor>[0-9]))" +
					"(?:(?<longMinor>[0-9]+)\\.|(?<shortMinor>[0-9]))" +
					"(?:(?<longPatch>[0-9]+)\\.|(?<shortPatch>[0-9]))?(-(?<identifier>[0-9A-Za-z]+))*");

	private final String name;
	private final String abbreviation;
	private final int major;
	private final int minor;
	private final int patch;

	private FrameworkVersion(String name, String abbreviation, int major, int minor, int patch) {
		this.name = name;
		this.abbreviation = abbreviation;
		this.major = major;
		this.minor = minor;
		this.patch = patch;
	}

	public String getName() {
		return name;
	}

	public String getAbbreviation() {
		return abbreviation;
	}


	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getPatch() {
		return patch;
	}

	public String versionedToken() {

		return abbreviation + major + "." + minor;
//		return abbreviation + major + minor + (patch != 0 ? patch : "");
	}

	public String versionedShortName() {

		return abbreviation + major + "." + minor + (patch != 0 ? "." + patch : "");
	}

	public String versionedFullName() {

		return name + major + "." + minor + (patch != 0 ? "." + patch : "");
	}

	@Override
	public String toString() {
		return "FrameworkVersion{" +
				"name='" + name + '\'' +
				", abbreviation='" + abbreviation + '\'' +
				", major=" + major +
				", minor=" + minor +
				", patch=" + patch +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FrameworkVersion that = (FrameworkVersion) o;
		return major == that.major &&
				minor == that.minor &&
				patch == that.patch &&
				Objects.equals(name, that.name) &&
				Objects.equals(abbreviation, that.abbreviation);
	}

	@Override
	public int hashCode() {

		return Objects.hash(name, abbreviation, major, minor, patch);
	}

	public boolean isDownwardsCompatible(FrameworkVersion other) {

		if (!Objects.equals(name, other.name)) {

			return false;
		}

		if (!Objects.equals(abbreviation, other.abbreviation)) {

			return false;
		}

		if (major < other.major) {

			return false;
		}

		if (major > other.major) {

			return true;
		}

		if (minor < other.minor) {

			return false;
		}

		if (minor > other.minor) {

			return true;
		}

		return patch >= other.patch;
	}

	public static FrameworkVersion newInstance(int major, int minor, int patch) {

		return newInstance(null, null, major, minor, patch);
	}

	public static FrameworkVersion newInstance(String name, String abbreviation, int major, int minor, int patch) {

		if (name == null || name.isEmpty()) {

			name = ".NETFramework";
		}

		if (abbreviation == null || abbreviation.isEmpty()) {

			abbreviation = "net";
		}


		return new FrameworkVersion(name, abbreviation, major, minor, patch);
	}

	public static FrameworkVersion defaultVersion() {

		return new FrameworkVersion(".NETStandard", "netstandard", 2, 0, 0);
	}

	public static FrameworkVersion fromShortName(String value) {

		Matcher matcher = VERSION_PATTERN.matcher(value);

		if (!matcher.matches()) {

			return null;
		}

		String abbreviation = matcher.group("abbreviation");

		try {

			int major = getArgument(matcher, "Major");
			int minor = getArgument(matcher, "Minor");
			int patch = getArgument(matcher, "Patch");

			return newInstance(getFullName(abbreviation), abbreviation, major, minor, patch);

		} catch (NumberFormatException e) {

			return null;
		}
	}

	private static int getArgument(Matcher matcher, String name) {

		String result = matcher.group("long" + name);

		if (result == null) {

			result = matcher.group("short" + name);
		}

		if (result == null) {

			return 0;
		}

		return Integer.parseInt(result);
	}

	private static String getFullName(String shortName) {

		String fullName = FULL_NAME_BY_SHORT_NAME.get(shortName);

		if (fullName != null) {

			return fullName;
		}

		// fall back to short name
		return shortName;
	}

	static {

		FULL_NAME_BY_SHORT_NAME.put("net", ".NETFramework");
		FULL_NAME_BY_SHORT_NAME.put("netcore", ".NETCoreApp");
		FULL_NAME_BY_SHORT_NAME.put("netfm", ".NETMicroFramework");
		FULL_NAME_BY_SHORT_NAME.put("win", "Windows");
		FULL_NAME_BY_SHORT_NAME.put("sl", "Silverlight");
		FULL_NAME_BY_SHORT_NAME.put("netstandard", ".NETStandard");

	}
}
