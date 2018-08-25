package com.github.cs;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This utility class helps handling streams.
 *
 * @author miracelwhipp
 */
public class Streams {

	private Streams() {
	}


	public static void unpackStream(InputStream source, File targetDirectory) throws IOException {

		makeDirectories(targetDirectory);

		try (ZipInputStream zipStream = new ZipInputStream(source)) {

			for (ZipEntry entry = zipStream.getNextEntry(); entry != null; entry = zipStream.getNextEntry()) {

				File targetFile = new File(targetDirectory, entry.getName());

				File parentFile = targetFile.getParentFile();

				makeDirectories(parentFile);

				try (FileOutputStream target = new FileOutputStream(targetFile)) {

					stream(zipStream, target, 128 * 1024);
				}

				zipStream.closeEntry();
			}
		}
	}

	private static void makeDirectories(File targetDirectory) {
		if (targetDirectory.exists()) {

			if (!targetDirectory.isDirectory()) {

				throw new IllegalArgumentException("target directory is file : " + targetDirectory.getAbsolutePath());
			}

		} else {

			if (!targetDirectory.mkdirs()) {

				throw new IllegalStateException("unable to create directory : " + targetDirectory.getAbsolutePath());
			}
		}
	}

	public static long stream(InputStream source, OutputStream target, int bufferSize) throws IOException {

		byte[] buffer = new byte[bufferSize];

		long completeSize = 0;
		int bytesRead = 0;

		while ((bytesRead = source.read(buffer)) > 0) {

			target.write(buffer, 0, bytesRead);
			completeSize += bytesRead;
		}

		return completeSize;
	}

	public static byte[] read(InputStream source, int bufferSize) throws IOException {

		try (ByteArrayOutputStream target = new ByteArrayOutputStream()) {

			stream(source, target, bufferSize);

			return target.toByteArray();
		}
	}

	public static void loadResource(Class<?> clazz, String resourceDir, String resourceName, File targetDirectory) throws IOException {

		FileUtils.forceMkdir(targetDirectory);

		try (
				InputStream source = clazz.getClassLoader().getResourceAsStream(resourceDir + "/" + resourceName);
				OutputStream target = new FileOutputStream(new File(targetDirectory, resourceName));
		) {

			if (source == null) {

				throw new FileNotFoundException("resource:" + resourceDir + "/" + resourceName);
			}

			Streams.stream(source, target, 512 * 1024);
		}

	}

	public static void copyResourceFile(Class<?> clazz, String resourceDir, String resourcePrefix, String resourceSuffix, File targetFile) throws IOException {

		File resourceFile = getResourceFile(clazz, resourceDir, resourcePrefix, resourceSuffix);

		if (resourceFile == null) {

			throw new FileNotFoundException("resource:" + resourceDir + "/" + resourcePrefix + "." + resourceSuffix);
		}

		Files.copy(resourceFile.toPath(), targetFile.toPath());
	}

	public static File getResourceFile(
			Class<?> clazz, String resourceDir, String resourcePrefix, String resourceSuffix) throws IOException {

		final URL resource =
				clazz.getClassLoader().getResource((resourceDir == null ? "" : resourceDir + "/") + resourcePrefix + "." + resourceSuffix);

		if (resource == null) {

			return null;
		}

		String protocol = resource.getProtocol();

		if (protocol.equals("file")) {

			return new File(resource.getFile());
		}

		if (!protocol.equals("jar")) {

			final File tempFile = File.createTempFile(resourcePrefix, resourceSuffix);

			try (
					InputStream source = resource.openStream();
					FileOutputStream target = new FileOutputStream(tempFile)
			) {
				Streams.stream(source, target, 128 * 1024);
			}

			return tempFile;
		}

		String fileString = resource.getPath().substring("file:".length(), resource.getPath().length() -
				("!/" + (resourceDir == null ? "" : resourceDir + "/") + resourcePrefix + "." + resourceSuffix).length());

		final File resourceFile = resourceDir == null
				? new File(resourcePrefix + "." + resourceSuffix)
				: new File(resourceDir, resourcePrefix + "." + resourceSuffix);

		File packageFile = new File(fileString);

		return unpackForFile(packageFile, resourceFile);
	}

	public static File unpackForFile(File packageFile, File resourceFile) throws IOException {

		if (resourceFile.isAbsolute()) {

			throw new IllegalStateException("resource file cannot be absolut. " + resourceFile.getPath());
		}

		File directory = new File(packageFile.getParentFile(), packageFile.getName() + ".unpack");

		resourceFile = new File(directory, resourceFile.getPath());

		if (resourceFile.exists() && resourceFile.lastModified() > packageFile.lastModified()) {

			return resourceFile;
		}

		try (FileInputStream source = new FileInputStream(packageFile)) {

			Streams.unpackStream(source, directory);
		}

		return resourceFile;
	}
}
