package com.castsoftware.dmt.discoverer.jee.netbeans;

import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xml.sax.Attributes;

import com.castsoftware.dmt.engine.discovery.IProjectsDiscovererUtilities;
import com.castsoftware.dmt.engine.project.Project;
import com.castsoftware.util.StringHelper;
import com.castsoftware.util.xml.AbstractXMLFileReader;
import com.castsoftware.util.xml.IInterpreter;

/**
 * Scanner for NetBeans project.xml files.
 */
public class ProjectFileScanner
{
    /**
     * Interpreter of a NetBeans project.xml file
     */
    public interface IProjectInterpreter extends IInterpreter
    {
    	/**
         * Adding a source folder to the list
         *
         * @param library
         *            the added sourceFolder
         */
    	void addWebModuleLibrary(String library);
        /**
         * Set the webModuleLibraries to the netbean project
         *
         */
    	void setWebModuleLibraries();

    	/**
         * Adding a source folder to the list
         *
         * @param sourceFolder
         *            the added sourceFolder
         */
    	void addWebModuleAdditionalLibrary(String library);
        /**
         * Set the webModuleAdditionalLibraries to the netbean project
         *
         */
    	void setWebModuleAdditionalLibraries();

    	/**
         * Adding a source folder to the list
         *
         * @param sourceFolder
         *            the added sourceFolder
         */
        void addSourceRoot(String root);
        /**
         * Set the sourceRoots to the netbean project
         *
         */
    	void setSourceRoots();
    	
    	/**
         * Adding a source folder to the list
         *
         * @param sourceFolder
         *            the added sourceFolder
         */
        void addTestRoot(String root);
        /**
         * Set the testRoots to the netbean project
         *
         */
    	void setTestRoots();

        /**
         * Interpret the type of the project
         *
         * @param type
         *            the type of the project
         */
        void projectType(String type);

        /**
         * Interpret the name of the project
         *
         * @param name
         *            the name of the project
         */
        void projectName(String name);

        /**
         * Adding an export to the list
         *
         * @param export
         *            the added export
         */
        void addExport(String export);

        /**
         * Set the exports to the netbean project
         *
         */
        void setExports();

        /**
         * Adding a source folder to the list
         *
         * @param sourceFolder
         *            the added sourceFolder
         */
        void addSourceFolder(String sourceFolder);

        /**
         * Set the source folders to the netbean project
         *
         */
        void setSourceFolders();

        /**
         * Adding a classpath defined in a compilation unit to the list
         *
         * @param classpath
         *            the added classpath (a jar file)
         */
        void addClasspath(String classpath);

        /**
         * Set the classpaths to the netbean project
         *
         */
        void setClasspaths();
    }

    private static class NetbeansProjectReader extends AbstractXMLFileReader
    {

        private IProjectInterpreter interpreter;
        private NetBeanProject netBeanProject;

        private boolean isInProject;
        private boolean isInConfiguration;
        private boolean isInGeneralData;
        // private boolean isInName;
        private boolean isInFolders;
        private boolean isInSourceFolder;
        private String sourceFolderType;
        private String sourceFolderLocation;
        private boolean isInExport;
        private String exportLocation;
        private boolean isInJavaData;
        private boolean isInCompilationUnit;
        private String classpath;
        private boolean isInData;
        private boolean isInWebModuleLibraries;
        private boolean isInWebModuleAdditionalLibraries;
        private boolean isInSourceRoots;
        private boolean isInTestRoots;
        private int level = 0;

        private NetbeansProjectReader()
        {
            // NOP
        }

        private NetBeanProject process(IProjectInterpreter projectInterpreter, String filePath, String content)
        {
            interpreter = projectInterpreter;
            
            isInProject = false;
            isInConfiguration = false;
            isInGeneralData = false;
            // isInName = false;
            isInFolders = false;
            isInSourceFolder = false;
            sourceFolderType = "";
            sourceFolderLocation = "";
            isInExport = false;
            exportLocation = "";
            isInJavaData = false;
            isInCompilationUnit = false;
            isInData = false;
            isInWebModuleLibraries = false;
            isInWebModuleAdditionalLibraries = false;
            isInSourceRoots = false;
            isInTestRoots = false;
            level = 0;


            StringReader reader = new StringReader(content);
            //boolean isOk = 
            readContents(interpreter, filePath, reader, false);

            interpreter = null;

            return netBeanProject;
        }

        @Override
        protected void startElement(String elementName, Attributes attributes)
        {
            if (!isInProject)
            {
                if ("project".equals(elementName))
                {
                    isInProject = true;

                    isInConfiguration = false;
                    
                    isInGeneralData = false;
                    // isInName = false;
                    isInFolders = false;
                    isInSourceFolder = false;
                    sourceFolderType = "";
                    sourceFolderLocation = "";
                    isInExport = false;
                    exportLocation = "";
                    isInJavaData = false;
                    isInCompilationUnit = false;
                    isInData = false;
                    level = 0;
                }
            }
            else
            {
            	level++;
            	if (level == 1 && "type".equals(elementName))
            		activateCharactersRecording();
            	else if (isInConfiguration)
            	{
            		if (isInGeneralData)
            		{
                        if (isInFolders)
                        {
	                        if (isInSourceFolder)
	                        {
	                            if (level == 5)
	                            {
	                            	if ("type".equals(elementName))
	                            		activateCharactersRecording();
		                            else if ("location".equals(elementName))
		                                activateCharactersRecording();
	                            }
	                        }
	                        else
	                        {
	                        	if (level == 4) {
		                            if ("source-folder".equals(elementName))
		                            {
		                                isInSourceFolder = true;
		                                sourceFolderType = "";
		                                sourceFolderLocation = "";
		                            }
	                        	}
	                        }
                        }
                        else if (isInExport)
                        {
                            if (level == 4)
                            {
                            	if ("type".equals(elementName))
                            		activateCharactersRecording();
                            	else if ("location".equals(elementName))
                            		activateCharactersRecording();
                            }
                        }
                        else
                        {
                            if (level == 3)
                            {
                            	if ("name".equals(elementName))
                            		activateCharactersRecording();
                                else if ("folders".equals(elementName))
                                    isInFolders = true;
                                else if ("export".equals(elementName))
                                {
                                    isInExport = true;
                                    exportLocation = "";
                                }
                            }
                        }
            		}
            		else if (isInJavaData)
                    {
                        if (isInCompilationUnit)
                        {
	                        if ("classpath".equals(elementName))
	                        	activateCharactersRecording();                	
                        }
                        else
                        {
                        	if ("compilation-unit".equals(elementName))
                        		isInCompilationUnit = true;
                        }
                    }
            		else if (isInData)
            		{
                        if (isInWebModuleLibraries)
                        {
                            if (level == 5)
                            {
		                        if ("file".equals(elementName))
		                        	activateCharactersRecording();
                            }
                        }
                        else if (isInWebModuleAdditionalLibraries)
                        {
                            if (level == 5)
                            {
                            	if ("file".equals(elementName))
                            		activateCharactersRecording();
                            }
                        }
                        else if (isInSourceRoots)
                        {
	                        if ("root".equals(elementName))
	                        {
	                        	String root = attributes.getValue(attributes.getIndex("id"));
	                        	interpreter.addSourceRoot(root);
	                        }
                        }
                        else if (isInTestRoots)
                        {
	                        if ("root".equals(elementName))
	                        {
	                        	String root = attributes.getValue(attributes.getIndex("id"));
	                        	interpreter.addTestRoot(root);
	                        }
                        }
                        else
                        {
                        	if (level == 3) 
                        	{
                        		if ("name".equals(elementName))
                        			activateCharactersRecording();
                        		else if ("web-module-libraries".equals(elementName))
	                        		isInWebModuleLibraries = true;
	                        	else if ("web-module-additional-libraries".equals(elementName))
	                        		isInWebModuleAdditionalLibraries = true;
	                        	else if ("source-roots".equals(elementName))
	                        		isInSourceRoots = true;
	                        	else if ("test-roots".equals(elementName))
	                        		isInTestRoots = true;
                        	}
                        }            			
            		}
                    else
                    {
                    	if (level == 2)
                    	{
	                        if ("general-data".equals(elementName))
	                            isInGeneralData = true;
	                        else if ("java-data".equals(elementName))
	                            isInJavaData = true;
	                        else if ("data".equals(elementName))
	                        	isInData = true;
                    	}
                    }
                }
                else
                {
                    if (level == 1 && "configuration".equals(elementName))
                        isInConfiguration = true;
                }
            }

        }

        @Override
        protected void endElement(String elementName)
        {
            if (!isInProject)
            	return;

            if (level == 0 && "project".equals(elementName))
            	isInProject = false;
            else if (level == 1 && "type".equals(elementName))
            {
            	interpreter.projectType(deactivateCharactersRecording());
            }
            else if (level == 1 && "configuration".equals(elementName))
                isInConfiguration = false;
            else if (level == 2)
            {
            	if ("general-data".equals(elementName))
	            	isInGeneralData = false;
	            else if ("java-data".equals(elementName))
	            {
	            	isInJavaData = false;
	            	interpreter.setClasspaths();
	            }
	            else if ("data".equals(elementName))
	            	isInData = false;
            }
            else if (level == 3)
            {
            	if ("compilation-unit".equals(elementName))
            	{
            		isInCompilationUnit = false;
            		// separator used in classpath can be either : or ;
            		List<String> classpathList = StringHelper.getStringList(classpath, ":");
            		for (String path : classpathList)
            		{
            			List<String> classpathList2 = StringHelper.getStringList(path, ";");
                		for (String path2 : classpathList2)
                			interpreter.addClasspath(path2);
            		}
                	classpath = "";
            	}
                else if (isInGeneralData)
                {
    	            if ("name".equals(elementName))
                        interpreter.projectName(deactivateCharactersRecording());
    	            else if (isInExport && "export".equals(elementName))
                    {
                        isInExport = false;
                        interpreter.addExport(exportLocation);
                    }
                    else if (isInFolders && "folders".equals(elementName))
                    {
                        isInFolders = false;
                        interpreter.setSourceFolders();
                    }
                }
                else if (isInData)
                {
    	            if ("name".equals(elementName))
                        interpreter.projectName(deactivateCharactersRecording());
    	            else if ("web-module-libraries".equals(elementName))
    	            {
                		isInWebModuleLibraries = false;
                		interpreter.setWebModuleLibraries();
    	            }
    	            else if ("web-module-additional-libraries".equals(elementName))
    	            {
                		isInWebModuleAdditionalLibraries = false;
                		interpreter.setWebModuleAdditionalLibraries();
    	            }
                	else if ("source-roots".equals(elementName))
                	{
                		isInSourceRoots = false;
                		interpreter.setSourceRoots();
                	}
                	else if ("test-roots".equals(elementName))
                	{
                		isInTestRoots = false;
    	            	interpreter.setTestRoots();
                	}
                }
            }
            else if (level == 4)
            {
            	if (isInCompilationUnit && "classpath".equals(elementName))
            	{
            		classpath = deactivateCharactersRecording();
            	}
            	else if (isInFolders && "source-folder".equals(elementName))
                {
                    isInSourceFolder = false;
                    if ("java".equals(sourceFolderType))
                        interpreter.addSourceFolder(sourceFolderLocation);
                }
                else if (isInExport)
                {
            		if ("location".equals(elementName))
                		exportLocation = deactivateCharactersRecording();
                }
            }
            else if (level == 5)
            {
                if (isInSourceFolder && "type".equals(elementName))
                    sourceFolderType = deactivateCharactersRecording();            	
                else if (isInSourceFolder && "location".equals(elementName))
                    sourceFolderLocation = deactivateCharactersRecording();
                else if (isInWebModuleLibraries && "file".equals(elementName))
                {
                    String library = deactivateCharactersRecording();
                    interpreter.addWebModuleLibrary(library);
                }
                else if (isInWebModuleAdditionalLibraries && "file".equals(elementName))
                {
                    String library = deactivateCharactersRecording();
                    interpreter.addWebModuleAdditionalLibrary(library);
                }
            }
            level--;

        }
    }

    private ProjectFileScanner()
    {
        // NOP
    }

    /**
     * Scan a .project file and add info to the project.
     *
     * @param interpreter
     *            the project file interpreter
     * @param projectFilePath
     *            the path to the project file used for reference
     * @param projectContent
     *            the file content to scan.
     * @return {@code true} if no error was encountered during scanning. {@code false} otherwise.
     */
    public static NetBeanProject scan(IProjectInterpreter interpreter, String projectFilePath, String projectContent)
    {
        NetbeansProjectReader reader = new NetbeansProjectReader();

        return reader.process(interpreter, projectFilePath, projectContent);
    }

    private static class ProjectRecorder implements IProjectInterpreter
    {
    	private NetBeanProject netBeanProject;
        private final Set<String> exports;
        private final Set<String> sourceFolders;
        private final Set<String> sourceRoots;
        private final Set<String> testRoots;
        private final Set<String> webModuleLibraries;
        private final Set<String> webModuleAdditionalLibraries;
        private final Set<String> classpaths;

        private ProjectRecorder(NetBeanProject netBeanProject)
        {
        	this.netBeanProject = netBeanProject;
            exports = new HashSet<String>();
            sourceFolders = new HashSet<String>();
            sourceRoots = new HashSet<String>();
            testRoots = new HashSet<String>();
            webModuleLibraries = new HashSet<String>();
            webModuleAdditionalLibraries = new HashSet<String>();
            classpaths = new HashSet<String>();
        }

        @Override
        public void init()
        {
            // ignore
        }

        @Override
        public void done()
        {
            // ignore
        }

        @Override
        public void open(String resourceId)
        {
            // ignore
        }

        @Override
        public void close()
        {
            // ignore
        }

        @Override
        public void projectName(String name)
        {
        	netBeanProject.setName(name);
        }

        @Override
        public void addExport(String export)
        {
            exports.add(export);
        }
		@Override
		public void setExports() {
			netBeanProject.setExports(exports);
		}

        @Override
        public void addSourceFolder(String sourceFolder)
        {
            sourceFolders.add(sourceFolder);
        }

        @Override
        public void setSourceFolders()
        {
        	netBeanProject.setSourceFolders(sourceFolders);
        }

		@Override
		public void addClasspath(String classpath) {
			classpaths.add(classpath);
		}

		@Override
		public void setClasspaths() {
			netBeanProject.setClasspaths(classpaths);
		}

		@Override
		public void projectType(String type) {
			netBeanProject.setType(type);			
		}

		@Override
    	public void addWebModuleLibrary(String library) {
			webModuleLibraries.add(library);
		}
		@Override
		public void setWebModuleLibraries() {
			netBeanProject.setWebModuleLibraries(webModuleLibraries);
		}

		@Override
    	public void addWebModuleAdditionalLibrary(String library) {
			webModuleAdditionalLibraries.add(library);
		}
		@Override
		public void setWebModuleAdditionalLibraries() {
			netBeanProject.setWebModuleAdditionalLibraries(webModuleAdditionalLibraries);
		}

		@Override
		public void addSourceRoot(String root) {
			sourceRoots.add(root);
		}
		@Override
		public void setSourceRoots() {
        	netBeanProject.setSourceRoots(sourceRoots);
		}

		@Override
		public void addTestRoot(String root) {
			testRoots.add(root);
		}
		@Override
		public void setTestRoots() {
        	netBeanProject.setTestRoots(testRoots);
		}
    }

    /**
     * Scan a .project file, add info to the project and return the project natures.
     *
     * @param project
     *            the project containing this file
     * @param projectContent
     *            the file content to scan.
     * @param javaLanguageId
     *            the java language ID to use to reference java files and folders.
     * @param javaContainerLanguageId
     *            the java container language ID to use to reference jar files or classpath.
     * @return null if an error was encountered during scanning. Otherwise a set containing the project natures.
     */
    public static NetBeanProject scan(Project project, String projectContent, IProjectsDiscovererUtilities projectsDiscovererUtilities, NetBeanProject netBeanProject)
    {
        IProjectInterpreter interpreter = new ProjectRecorder(netBeanProject);
        return scan(interpreter, project.getPath(), projectContent);
    }

}
