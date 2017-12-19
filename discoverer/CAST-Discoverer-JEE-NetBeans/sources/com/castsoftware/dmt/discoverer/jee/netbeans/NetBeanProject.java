package com.castsoftware.dmt.discoverer.jee.netbeans;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class NetBeanProject {
	private String projectPath;
	private String type;
	private String name;
	private Set<String> webModuleLibraries;
	private Set<String> webModuleAdditionalLibraries;
	private Set<String> sourceRoots;
	private Set<String> testRoots;
	private Set<String> sourceFolders;
	private Set<String> exports;
	private Set<String> classpaths;
	
	public NetBeanProject(String projectPath)
	{
		setProjectPath(projectPath);
		setName(null);
		webModuleLibraries = new HashSet<String>();
		webModuleAdditionalLibraries = new HashSet<String>();
		sourceRoots = new HashSet<String>();
		testRoots = new HashSet<String>();
		sourceFolders = new HashSet<String>();
		exports = new HashSet<String>();
		classpaths = new HashSet<String>();
	}
	public String getProjectPath() {
		return projectPath;
	}

	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	public Set<String> getWebModuleLibraries() {
		return webModuleLibraries;
	}

	public void setWebModuleLibraries(Set<String> webModuleLibraries) {
		this.webModuleLibraries = webModuleLibraries;
	}

	public Set<String> getWebModuleAdditionalLibraries() {
		return webModuleAdditionalLibraries;
	}

	public void setWebModuleAdditionalLibraries(
			Set<String> webModuleAdditionalLibraries) {
		this.webModuleAdditionalLibraries = webModuleAdditionalLibraries;
	}

	public Set<String> getSourceRoots() {
		return sourceRoots;
	}

	public void setSourceRoots(Set<String> roots) {
		for (String root : roots)
			this.sourceRoots.add(root);
	}

	public Set<String> getTestRoots() {
		return testRoots;
	}

	public void setTestRoots(Set<String> testRoots) {
		this.testRoots = testRoots;
	}
	
	public Set<String> getSourceFolders() {
		return sourceFolders;
	}
	public void setSourceFolders(Set<String> sourceFolders) {
		this.sourceFolders = sourceFolders;
	}

	public Set<String> getExports() {
		return exports;
	}
	public void setExports(Set<String> exports) {
		this.exports = exports;
	}

	public void resolveProperties(Properties props) {
		for (String key : props.stringPropertyNames()) {
			String value = props.getProperty(key);			
			resolveProperty(key,value,props, 0);
		}
		return;
	}
	
	private void resolveProperty(String key, String value, Properties props, int skipIndex) {
		int beginIndex = value.indexOf("${",skipIndex);
		if (beginIndex >= 0)
		{
			int endIndex = value.indexOf("}", beginIndex);
			int len = value.length() - 1;
			if (endIndex >= 0)
			{
				String varName = value.substring(beginIndex + 2,endIndex);
				String varValue = props.getProperty(varName);
				if (varValue != null)
				{
					if (beginIndex == 0)
						if (endIndex == len)
							value = varValue;
						else
							value = varValue + value.substring(endIndex + 1);
					else
						if (endIndex == len)
							value = value.substring(0, beginIndex) + varValue;
						else
							value = value.substring(0, beginIndex) + varValue + value.substring(endIndex + 1);
					resolveProperty(key, value, props, beginIndex);
				}
				else
					resolveProperty(key, value, props, beginIndex + 1);
			}
			else
				props.setProperty(key, value);
		}
		else
			props.setProperty(key, value);
		return;
	}
	public Set<String> getClasspaths() {
		return classpaths;
	}
	public void setClasspaths(Set<String> classpaths) {
		this.classpaths = classpaths;
	}
}
