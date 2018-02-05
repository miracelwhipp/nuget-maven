package com.github.cs;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import java.util.Map;

/**
 * TODO: document me
 *
 * @author miracelwhipp
 */
@Component(role = TestRunnerFactoryRegistry.class)
public class TestRunnerFactoryRegistry {

	@Requirement(role = NetTestRunnerFactory.class)
	private Map<String, NetTestRunnerFactory> factories;

	public NetTestRunnerFactory getFactory() {

		return factories.get("nunit");
	}

}
