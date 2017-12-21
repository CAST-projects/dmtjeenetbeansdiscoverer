package com.castsoftware.dmt.discoverer.jee.netbeans;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.castsoftware.dmt.engine.discovery.BasicProjectsDiscovererAdapter;
import com.castsoftware.dmt.engine.discovery.IProjectsDiscovererUtilities;
import com.castsoftware.dmt.engine.discovery.ProjectsDiscovererWrapper.ProfileOrProjectTypeConfiguration.LanguageConfiguration;
import com.castsoftware.dmt.engine.project.IProfileReadOnly;
import com.castsoftware.dmt.engine.project.IResourceReadOnly;
import com.castsoftware.dmt.engine.project.Profile;
import com.castsoftware.dmt.engine.project.Project;
import com.castsoftware.dmt.engine.project.Profile.IReferencedContents;
import com.castsoftware.dmt.engine.project.Profile.ResourceReference;
import com.castsoftware.util.StringHelper;
import com.castsoftware.util.logger.Logging;

/**
 * Basic discoverer for
 */
public class JeeNetbeansProjectsDiscoverer extends BasicProjectsDiscovererAdapter
{
    private final Map<String,NetBeanProject> netbeanProjects;
    private static final int JAVA_LANGUAGE_ID = 1;
    private static final int JAVA_CONTAINER_LANGUAGE_ID = 1;
    private static final int JAVA_XML_LANGUAGE_ID = 2;
    private static final int JAVA_PROPERTIES_LANGUAGE_ID = 3;
    private Boolean isIncludeTest = false;
    private Boolean isConfigurationChecked = false;
    private Boolean isConfigurationGood = false;

    /**
     * Default constructor used by the discovery engine
     */
    public JeeNetbeansProjectsDiscoverer()
    {
    	netbeanProjects = new HashMap<String, NetBeanProject>();
    }

    private void checkConfiguration(Iterable<LanguageConfiguration> languageConfigurations)
    {
		int javaLanguageId = -1;
		int javaContainerLanguageId = -1;
	
	    for (LanguageConfiguration languageConfiguration : languageConfigurations)
	    {
	        int languageId = languageConfiguration.getLanguageId();
	        if ("JavaLanguage".equals(languageConfiguration.getLanguageName()))
	        {
	        	javaLanguageId = languageId;
	        	//TODO: not available in 7.3.x API => hardcoded value
	        	/*
	            if ("JavaContainerLanguage".equals(languageConfiguration.getLanguageName()))
	            	javaContainerLanguageId = languageId;
	            */
	            if (javaContainerLanguageId == -1)
	            {
	            	javaContainerLanguageId = 1;
	                //Logging.managedError("cast.dmt.discover.jee.netbeans.getJavaContainerLanguageFailure");
	            }
	        	break;
	        }
	    }
	    if (javaLanguageId == -1)
	    {
	        Logging.managedError("cast.dmt.discover.jee.netbeans.getJavaLanguageFailure");
	        isConfigurationGood = false;
	    }
	    isConfigurationGood = true;
    }
    
    @Override
    public void buildProject(String relativeFilePath, String content, Project project,
        IProjectsDiscovererUtilities projectsDiscovererUtilities)
    {
    	Logging.info("cast.dmt.discover.jee.netbeans.buildProjectStart", "PATH", project.getPath());
    	if (!isConfigurationChecked)
    	{
    		Iterable<LanguageConfiguration> languageConfigurations = projectsDiscovererUtilities.getProjectTypeConfiguration(project.getType()).getLanguageConfigurations();
        	checkConfiguration(languageConfigurations);
        	isConfigurationChecked = true;
    	}
    	if (!isConfigurationGood)
    	{
    		projectsDiscovererUtilities.deleteProject(project.getId());
        	return;
    	}
    		
    	String projectDescriptor = project.getMetadata(IProfileReadOnly.METADATA_DESCRIPTOR).getValue();
        if ((!projectDescriptor.equals("project.xml")) )
        {
        	projectsDiscovererUtilities.deleteProject(project.getId());
        	return;
        }
        NetBeanProject netBeanProject = new NetBeanProject(project.getPath());
        ProjectFileScanner.scan(project, content, projectsDiscovererUtilities, netBeanProject);
        if (netBeanProject.getType() == null)
        {
        	projectsDiscovererUtilities.deleteProject(project.getId());
        	return;
        }
        else
        {
        	if (!("org.netbeans.modules.java.j2seproject".equals(netBeanProject.getType())
        			|| "org.netbeans.modules.ant.freeform".equals(netBeanProject.getType())
        			|| "org.netbeans.modules.web.project".equals(netBeanProject.getType())))
        		Logging.warn("cast.dmt.discover.jee.netbeans.unknownType", "TYPE", netBeanProject.getType());
        }
        project.setName(netBeanProject.getName());
        project.addMetadata(IResourceReadOnly.METADATA_REFKEY, netBeanProject.getName());
        netbeanProjects.put(project.getPath(), netBeanProject);
    	addProjectRelativeFileRef(project, "project.properties");
    	Logging.info("cast.dmt.discover.jee.netbeans.buildProjectEnd", "PATH", project.getPath());
    }

    private static ResourceReference addProjectRelativeFileRef(Project project, String path)
    {
        String fullPath = project.buildPackageRelativePath(path);
        //String fullPath = project.getPath() + "/" + path;

        return project.addFileReference(fullPath, Project.PROJECT_LANGUAGE_ID, IResourceReadOnly.RESOURCE_TYPE_NEUTRAL_ID);
    }
    
    @Override
    public boolean reparseProject(Project project, String projectContent, IReferencedContents contents,
        IProjectsDiscovererUtilities projectsDiscovererUtilities)
    {
    	Logging.info("cast.dmt.discover.jee.netbeans.reparseProjectStart", "PATH", project.getPath());
        String projectProperties = project.buildPackageRelativePath("project.properties");
        String projectPropertiesContent = contents.getContent(projectProperties);
    	NetBeanProject netBeanProject = netbeanProjects.get(project.getPath());
    	if (netBeanProject != null)
    	{
            Properties props = null;
            if (StringHelper.isEmpty(projectPropertiesContent))
                Logging.info("cast.dmt.discover.jee.netbeans.missingProjectProperties", "PROJECT_PATH", project.getPath());
            else
            {
        		props = readProjectProperties(netBeanProject, projectPropertiesContent);
        	}
    		finalizeProject(project,netBeanProject, props);
        }
        project.removeResourceReference(projectPropertiesContent);
    	Logging.info("cast.dmt.discover.jee.netbeans.reparseProjectEnd", "PATH", project.getPath());
    	return false;
    }
    
    private Properties readProjectProperties(NetBeanProject project, String projectPropertiesContent)
    {
    	Properties props = new Properties();
    	StringReader reader = new StringReader(projectPropertiesContent);

    	try {
			props.load(reader);
			project.resolveProperties(props);
		} catch (IOException e) {
			Logging.warn("cast.dmt.discover.eclipse.jee.classpathFileFound", "PROJECT_PATH", project.getProjectPath());
		}
    	return props;
    }
    private void finalizeProject(Project project, NetBeanProject netBeanProject, Properties props)
    {
    	project.setName(netBeanProject.getName());
    	project.addMetadata(IResourceReadOnly.METADATA_REFKEY, netBeanProject.getName());
    	
    	if (netBeanProject.getSourceRoots() != null)
	        for (String sourceRoot : netBeanProject.getSourceRoots())
	        {
	        	String path = resolveProperties(sourceRoot,props);
	        	if (isFullPathOrVariable(path))
	        		project.addSourceDirectoryReference(path, JAVA_LANGUAGE_ID);
	        	else
	        		project.addSourceDirectoryReference(buildPackageRelativePath(project, "../".concat(path)), JAVA_LANGUAGE_ID);
	        }
        if (isIncludeTest)
        	if (netBeanProject.getTestRoots() != null)
	        	for (String testRoot : netBeanProject.getTestRoots())
	        	{
	        		String path = resolveProperties(testRoot,props);
		        	if (isFullPathOrVariable(path))
		        		project.addSourceDirectoryReference(path, JAVA_LANGUAGE_ID);
		        	else
		        		project.addSourceDirectoryReference(buildPackageRelativePath(project, "../".concat(path)), JAVA_LANGUAGE_ID);
	        	}
        if (netBeanProject.getSourceFolders() != null)
        	for (String sourceFolder : netBeanProject.getSourceFolders())
        	{
        		String path = resolveProperties(sourceFolder,props);
	        	if (isFullPathOrVariable(path))
	        		project.addSourceDirectoryReference(path, JAVA_LANGUAGE_ID);
	        	else
	        		project.addSourceDirectoryReference(buildPackageRelativePath(project, "../".concat(path)), JAVA_LANGUAGE_ID);
        	}

        if (netBeanProject.getWebModuleLibraries() != null)
	        for (String library : netBeanProject.getWebModuleLibraries())
	        {
        		String path = resolveProperties(library,props);
	        	if (path.toLowerCase().endsWith(".jar"))
	        	{
		        	if (isFullPathOrVariable(path))
		        		project.addContainerReference(path, JAVA_LANGUAGE_ID, JAVA_CONTAINER_LANGUAGE_ID);
		        	else
		        		project.addContainerReference(buildPackageRelativePath(project, "../".concat(path)), JAVA_LANGUAGE_ID, JAVA_CONTAINER_LANGUAGE_ID);
	        	}
	        	else
	        	{
    	        	if (isFullPathOrVariable(path))
    	        		project.addDirectoryReference(path, JAVA_LANGUAGE_ID, JAVA_CONTAINER_LANGUAGE_ID);
    	        	else
    	        		project.addDirectoryReference(buildPackageRelativePath(project, "../".concat(path)), JAVA_LANGUAGE_ID, JAVA_CONTAINER_LANGUAGE_ID);
	        	}
	        }
        if (netBeanProject.getWebModuleAdditionalLibraries() != null)
	        for (String library : netBeanProject.getWebModuleAdditionalLibraries())
        	{
        		String path = resolveProperties(library,props);
	        	if (path.toLowerCase().endsWith(".jar"))
	        	{
		        	if (isFullPathOrVariable(path))
		        		project.addContainerReference(path, JAVA_LANGUAGE_ID, JAVA_CONTAINER_LANGUAGE_ID);
		        	else
		        		project.addContainerReference(buildPackageRelativePath(project, "../".concat(path)), JAVA_LANGUAGE_ID, JAVA_CONTAINER_LANGUAGE_ID);
	        	}
	        	else
	        	{
    	        	if (isFullPathOrVariable(path))
    	        		project.addDirectoryReference(path, JAVA_LANGUAGE_ID, JAVA_CONTAINER_LANGUAGE_ID);
    	        	else
    	        		project.addDirectoryReference(buildPackageRelativePath(project, "../".concat(path)), JAVA_LANGUAGE_ID, JAVA_CONTAINER_LANGUAGE_ID);
	        	}
        	}
        if (netBeanProject.getClasspaths() != null)
	        for	 (String classpath : netBeanProject.getClasspaths())
	        {
        		String path = resolveProperties(classpath,props);
	        	if (path.toLowerCase().endsWith(".jar"))
	        	{
		        	if (isFullPathOrVariable(path))
		        		project.addContainerReference(path, JAVA_LANGUAGE_ID, JAVA_CONTAINER_LANGUAGE_ID);
		        	else
		        		project.addContainerReference(buildPackageRelativePath(project, "../".concat(path)), JAVA_LANGUAGE_ID, JAVA_CONTAINER_LANGUAGE_ID);
	        	}
	        	else
	        	{
    	        	if (isFullPathOrVariable(path))
    	        		project.addDirectoryReference(path, JAVA_LANGUAGE_ID, JAVA_CONTAINER_LANGUAGE_ID);
    	        	else
    	        		project.addDirectoryReference(buildPackageRelativePath(project, "../".concat(path)), JAVA_LANGUAGE_ID, JAVA_CONTAINER_LANGUAGE_ID);
	        	}
	        }
        return;
    }
    private String resolveProperties(String value, Properties props)
    {
    	if (props == null)
    		return value;
    	if (value.contains("${"))
    	{
    		int beginIndex = value.indexOf("${");
    		while (beginIndex >= 0)
    		{
    			int endIndex = value.indexOf("}",beginIndex);
    			if (endIndex > 0)
    			{
    				String varName = stripTokens(value.substring(beginIndex, endIndex + 1));
    				String varValue = props.getProperty(varName);
    				if (varValue != null)
    					value = value.replace("${"+varName+"}",varValue);
    				else
    					value = value.replace("${"+varName+"}","%"+varName+"%");
    			}
    			else
    				break;
    			beginIndex = value.indexOf("${", beginIndex + 1);
    		}
    	}
    	else
    	{
    		String prop = props.getProperty(value);
    		if (prop != null)
    			return prop;
    	}
    	return value;
    }
    private static Boolean isFullPathOrVariable(String path)
    {
    	if (path.startsWith("//"))
    		return true;
    	else if (path.substring(1,3).equals(":\\"))
    		return true;
    	else if (path.contains("%"))
    		return true;
    	return false;
    }
    /**
     * Extract the name of a variable
     *
     * @param expr
     *            The expression defining the variable
     * @return The raw name of the variables
     */
    private static String stripTokens(String expr)
    {
        if (expr.startsWith("${") && (expr.indexOf("}") == expr.length() - 1))
        {
            expr = expr.substring(2, expr.length() - 1);
        }
        return expr;
    }

    private static String buildPackageRelativePath(Project project, String projectPath)
    {
        if (projectPath.startsWith("/"))
        {
            int slashPos = projectPath.indexOf('/', 1);
            if (slashPos == -1)
                return projectPath.substring(1);

            String projectRelativePath = projectPath.substring(slashPos + 1);
            String projectName = projectPath.substring(1, slashPos);

            return Profile.buildPackageRelativePath(projectName, projectRelativePath);
        }

        if (new File(projectPath).isAbsolute())
            return projectPath;

        return project.buildPackageRelativePath(projectPath);
    }
}
