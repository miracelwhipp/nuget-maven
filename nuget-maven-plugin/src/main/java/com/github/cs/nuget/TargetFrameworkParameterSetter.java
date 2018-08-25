package com.github.cs.nuget;

import com.github.cs.Xml;

import javax.xml.transform.Transformer;

/**
 * TODO: document me
 *
 * @author miracelwhipp
 */
class TargetFrameworkParameterSetter implements Xml.ParameterSetter {

	private final String targetFramework;

	TargetFrameworkParameterSetter(String targetFramework) {
		this.targetFramework = targetFramework;
	}

	@Override
	public void setParameters(Transformer transformer) {
		transformer.setParameter("targetFramework", targetFramework);
	}
}
