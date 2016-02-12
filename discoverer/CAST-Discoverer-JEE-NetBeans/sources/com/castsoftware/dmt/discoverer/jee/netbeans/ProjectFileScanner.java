package com.castsoftware.dmt.discoverer.jee.netbeans;

import java.io.File;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xml.sax.Attributes;

import com.castsoftware.dmt.engine.discovery.ProjectsDiscovererWrapper.ProfileOrProjectTypeConfiguration.LanguageConfiguration;
import com.castsoftware.dmt.engine.project.IResourceReadOnly;
import com.castsoftware.dmt.engine.project.Profile;
import com.castsoftware.dmt.engine.project.Profile.Language;
import com.castsoftware.dmt.engine.project.Project;
import com.castsoftware.util.StringHelper;
import com.castsoftware.util.logger.Logging;
import com.castsoftware.util.xml.AbstractXMLFileReader;
import com.castsoftware.util.xml.IInterpreter;

/**
 * Scanner for Eclipse .project files.
 */
public class ProjectFileScanner
{
    /**
     * Interpreter of an eclipse .project file
     */
    public interface IProjectInterpreter extends IInterpreter
    {

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
         * Adding a source folder to the list
         *
         * @param sourceFolder
         *            the added sourceFolder
         */
        void addSourceFolder(String sourceFolder);

        /**
         * Adding the source folders to the project
         *
         */
        void addProjectSourceFolders();

        /**
         * Adding a classpath defined in a compilation unit to the list
         *
         * @param classpath
         *            the added classpath (a jar file)
         */
        void addClasspath(String classpath);

        /**
         * Adding the classpaths to the project
         *
         */
        void addClasspaths();
}

    private static class NetbeansProjectReader extends AbstractXMLFileReader
    {

        private IProjectInterpreter interpreter;

        private boolean isInProject;
        private boolean isInConfiguration;
        private boolean isInGeneralData;
        // private boolean isInName;
        private boolean isInFolders;
        private boolean isInSourceFolder;
        private String sourceFolderType;
        private String sourceFolderLocation;
        private boolean isInExport;
        private String exportType;
        private String exportLocation;
        private boolean isInJavaData;
        private boolean isInCompilationUnit;
        private String classpath;
        private int ignoredDepth;

        private NetbeansProjectReader()
        {
            // NOP
        }

        private boolean process(IProjectInterpreter projectInterpreter, String filePath, String content)
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
            exportType = "";
            exportLocation = "";
            isInJavaData = false;
            isInCompilationUnit = false;
            ignoredDepth = 0;


            StringReader reader = new StringReader(content);
            boolean isOk = readContents(interpreter, filePath, reader, false);

            interpreter = null;

            return isOk;
        }

        @Override
        protected void startElement(String elementName, Attributes attributes)
        {
            if (ignoredDepth > 0)
                ignoredDepth++;
            else if (!isInProject)
            {
                if ("project".equals(elementName))
                    isInProject = true;
                else
                    ignoredDepth = 1;
            }
            else if (!isInConfiguration)
            {
                if ("configuration".equals(elementName))
                    isInConfiguration = true;
            }
            else if (!isInGeneralData)
            {
                if ("general-data".equals(elementName))
                    isInGeneralData = true;
                else if (!isInJavaData)
                {
                	if ("java-data".equals(elementName))
                        isInJavaData = true;
                }
                else
                {
                    if (!isInCompilationUnit)
                    {
                    	if ("compilation-unit".equals(elementName))
                    		isInCompilationUnit = true;
                    }
                    else if ("classpath".equals(elementName))
                    	activateCharactersRecording();                	
                }
            }
            else
            {
                if (!isInFolders)
                {
                    if ("folders".equals(elementName))
                        isInFolders = true;
                }
                else if (!isInSourceFolder)
                {
                    if ("source-folder".equals(elementName))
                    {
                        isInSourceFolder = true;
                        sourceFolderType = "";
                        sourceFolderLocation = "";
                    }
                    else
                        ignoredDepth = 1;
                }
                else
                {
                    if ("type".equals(elementName))
                        activateCharactersRecording();
                    else if ("location".equals(elementName))
                        activateCharactersRecording();
                }

                if (!isInExport)
                {
                    if ("export".equals(elementName))
                    {
                        isInExport = true;
                        exportType = "";
                        exportLocation = "";
                    }
                }
                else
                {
                    if ("type".equals(elementName))
                        activateCharactersRecording();
                    else if ("location".equals(elementName))
                        activateCharactersRecording();
                }

                if ("name".equals(elementName))
                    activateCharactersRecording();
            }
        }

        @Override
        protected void endElement(String elementName)
        {
            if (ignoredDepth > 0)
                ignoredDepth--;
            else if (!isInProject)
            {
                // NOP
            }
            else if (!isInConfiguration)
            {
                if ("project".equals(elementName))
                    isInProject = false;
            }
            else if (!isInGeneralData)
            {
                if (!isInJavaData)
                {
                	if ("configuration".equals(elementName))
                        isInConfiguration = false;
                }
                else
                {
                	if (!isInCompilationUnit)
                	{
                		if ("java-data".equals(elementName))
                		{
                        	isInJavaData = false;
                        	interpreter.addClasspaths();
                		}
                	}
                	else if ("compilation-unit".equals(elementName))
                	{
                		isInCompilationUnit = false;
                		// TODO: split the list
                		List<String> classpathList = StringHelper.getStringList(classpath, ":");
                		for (String path : classpathList)
                			interpreter.addClasspath(path);
                	}
                	else if ("classpath".equals(elementName))
                	{
                		classpath = deactivateCharactersRecording();
                	}
                }                
            }
            else if (!isInFolders)
            {
                if ("general-data".equals(elementName))
                    isInGeneralData = false;
                else if ("export".equals(elementName))
                {
                    isInExport = false;
                    interpreter.addExport(exportLocation);
                }
            }
            else if (!isInSourceFolder)
            {
                if ("folders".equals(elementName))
                {
                    isInFolders = false;
                    interpreter.addProjectSourceFolders();
                }
            }
            else
            {
                if ("source-folder".equals(elementName))
                {
                    isInSourceFolder = false;
                    if ("java".equals(sourceFolderType))
                        interpreter.addSourceFolder(sourceFolderLocation);
                }
            }
            if (isInGeneralData && "name".equals(elementName))
                interpreter.projectName(deactivateCharactersRecording());
            else if (isInSourceFolder && "type".equals(elementName))
                sourceFolderType = deactivateCharactersRecording();
            else if (isInSourceFolder && "location".equals(elementName))
                sourceFolderLocation = deactivateCharactersRecording();
            else if (isInExport && "type".equals(elementName))
                exportType = deactivateCharactersRecording();
            else if (isInExport && "location".equals(elementName))
                exportLocation = deactivateCharactersRecording();

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
    public static boolean scan(IProjectInterpreter interpreter, String projectFilePath, String projectContent)
    {
        NetbeansProjectReader reader = new NetbeansProjectReader();

        return reader.process(interpreter, projectFilePath, projectContent);
    }

    private static class ProjectRecorder implements IProjectInterpreter
    {

        private final Project project;
        private final Set<String> exports;
        private final Set<String> sourceFolders;
        private final Set<String> classpaths;
        private int javaLanguageId = -1;
        private int JavaContainerLanguageId = -1;

        private ProjectRecorder(Project project, Set<String> exports)
        {
            this.project = project;
            this.exports = exports;
            sourceFolders = new HashSet<String>();
            classpaths = new HashSet<String>();
            /*
            for (LanguageConfiguration languageConfiguration : getProjectsDiscovererUtilities().getProjectTypeConfiguration(
                    project.getType()).getLanguageConfigurations())
                {
                    int languageId = languageConfiguration.getLanguageId();
                    if ("javaLanguage".equals(languageConfiguration.getLanguageName()))
                    	javaLanguageId = languageId;
                    if ("javaContainerLanguage".equals(languageConfiguration.getLanguageName()))
                    	JavaContainerLanguageId = languageId;
                }
            */
            for (Language language : project.getLanguages())
            {
                if ("JavaLanguage".equals(language.getName()))
                {
                    javaLanguageId = language.getId();
                    
                    break;
                }
            }
            if (javaLanguageId == -1)
            {
                javaLanguageId = 1;
                Logging.warn("cast.dmt.discover.jee.netbeans.getJavaLanguageFailure");
            }
            if (JavaContainerLanguageId == -1)
            {
            	JavaContainerLanguageId = 2;
                //Logging.warn("cast.dmt.discover.jee.netbeans.getJavaContainerLanguageFailure");
            }
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
            project.setName(name);
            project.addMetadata(IResourceReadOnly.METADATA_REFKEY, name);
        }

        @Override
        public void addExport(String export)
        {
            exports.add(export);

        }

        @Override
        public void addSourceFolder(String sourceFolder)
        {
            sourceFolders.add(sourceFolder);
        }

        @Override
        public void addProjectSourceFolders()
        {
            for (String sourceFolder : sourceFolders)
                project.addSourceDirectoryReference(buildPackageRelativePath(project, "../".concat(sourceFolder)), javaLanguageId);
        }

		@Override
		public void addClasspath(String classpath) {
			classpaths.add(classpath);
		}

		@Override
		public void addClasspaths() {
            for (String path : classpaths)
            {
                if (path.toLowerCase().endsWith(".jar"))
                    project.addContainerReference(buildPackageRelativePath(project, "../".concat(path)), javaLanguageId,
                        JavaContainerLanguageId);
                else
                    project.addDirectoryReference(buildPackageRelativePath(project, "../".concat(path)), javaLanguageId,
                        JavaContainerLanguageId);
            }
		}
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

    /**
     * Scan a .project file, add info to the project and return the project natures.
     *
     * @param project
     *            the project containing this file
     * @param projectContent
     *            the file content to scan.
     * @return null if an error was encountered during scanning. Otherwise a set containing the project natures.
     */
    public static Set<String> scan(Project project, String projectContent)
    {

        Set<String> exports = new HashSet<String>();

        IProjectInterpreter interpreter = new ProjectRecorder(project, exports);
        if (!scan(interpreter, project.getPath(), projectContent))
            return null;
        return exports;
    }

}
