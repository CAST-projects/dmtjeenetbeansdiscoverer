package com.castsoftware.dmt.discoverer.jee.netbeans;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Data for a NetBeans project
 */
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
	/**
	 * Path of the NetBeans project
     * @return the project path
	 */
	public String getProjectPath() {
		return projectPath;
	}
	/**
	 * Setter for path of the NetBeans project
     * @param projectPath
     *            the path of the file
	 */
	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath;
	}
	/**
	 * Path of the NetBeans project
     * @return the project path
	 */
	public String getType() {
		return type;
	}
	/**
	 * Setter for type of the NetBeans project
     * @param type
     *            the type
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * Name of the NetBeans project
     * @return the project path
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setter for name of the NetBeans project
     * @param name
     *            the name
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * List of Web Module Libraries for the NetBeans project
     * @return the list
	 */
	public Set<String> getWebModuleLibraries() {
		return webModuleLibraries;
	}

	/**
	 * Setter for Web Module Libraries of the NetBeans project
     * @param webModuleLibraries
     *            the list
	 */
	public void setWebModuleLibraries(Set<String> webModuleLibraries) {
		this.webModuleLibraries = webModuleLibraries;
	}

	/**
	 * List of Web Module Additional Libraries for the NetBeans project
     * @return the list
	 */
	public Set<String> getWebModuleAdditionalLibraries() {
		return webModuleAdditionalLibraries;
	}

	/**
	 * Setter for Web Module Additional Libraries of the NetBeans project
     * @param webModuleAdditionalLibraries
     *            the list
	 */
	public void setWebModuleAdditionalLibraries(
			Set<String> webModuleAdditionalLibraries) {
		this.webModuleAdditionalLibraries = webModuleAdditionalLibraries;
	}

	/**
	 * List of source-roots for the NetBeans project
     * @return the list
	 */
	public Set<String> getSourceRoots() {
		return sourceRoots;
	}

	/**
	 * Setter for source-roots of the NetBeans project
     * @param roots
     *            the list
	 */
	public void setSourceRoots(Set<String> roots) {
		for (String root : roots)
			this.sourceRoots.add(root);
	}

	/**
	 * List of test-roots for the NetBeans project
     * @return the list
	 */
	public Set<String> getTestRoots() {
		return testRoots;
	}

	/**
	 * Setter for test-roots of the NetBeans project
     * @param testRoots
     *            the list
	 */
	public void setTestRoots(Set<String> testRoots) {
		this.testRoots = testRoots;
	}
	
	/**
	 * List of source folders for the NetBeans project
     * @return the list
	 */
	public Set<String> getSourceFolders() {
		return sourceFolders;
	}
	/**
	 * Setter for source folders of the NetBeans project
     * @param sourceFolders
     *            the list
	 */
	public void setSourceFolders(Set<String> sourceFolders) {
		this.sourceFolders = sourceFolders;
	}

	/**
	 * List of exports for the NetBeans project
     * @return the list
	 */
	public Set<String> getExports() {
		return exports;
	}
	/**
	 * Setter for exports of the NetBeans project
     * @param exports
     *            the list
	 */
	public void setExports(Set<String> exports) {
		this.exports = exports;
	}

	/**
	 * Replace the variables by their value in the date of the NetBeans project
     * @param props
     *            the list of properties defined in the project.properties files
	 */
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
	/**
	 * List of classpaths for the NetBeans project
     * @return the list
	 */
	public Set<String> getClasspaths() {
		return classpaths;
	}
	/**
	 * Setter for classpaths of the NetBeans project
     * @param classpaths
     *            the list
	 */
	public void setClasspaths(Set<String> classpaths) {
		this.classpaths = classpaths;
	}
}
