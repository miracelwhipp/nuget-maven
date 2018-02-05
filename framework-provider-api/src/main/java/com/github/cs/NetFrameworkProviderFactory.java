package com.github.cs;

import java.io.File;
import java.util.Map;

/**
 * This service provider interface describes how to create a new {@link NetFrameworkProvider}.
 *
 * @author miracelwhipp
 */
public interface NetFrameworkProviderFactory {


	/**
	 * This method creates a new {@link NetFrameworkProvider}. Generic configuration can be given depending on the
	 * concrete implementation.
	 *
	 * @param configuration the configuration of the provider to create
	 * @return a new NetFrameworkProvider
	 */
	NetFrameworkProvider newFrameworkProvider(Map<String, String> configuration);

	FrameworkVersion getFrameworkVersion();

}
