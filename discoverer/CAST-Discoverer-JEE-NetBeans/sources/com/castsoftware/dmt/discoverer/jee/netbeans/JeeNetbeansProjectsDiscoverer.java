package com.castsoftware.dmt.discoverer.jee.netbeans;

import java.util.Set;

import com.castsoftware.dmt.engine.discovery.BasicProjectsDiscovererAdapter;
import com.castsoftware.dmt.engine.discovery.IProjectsDiscovererUtilities;
import com.castsoftware.dmt.engine.project.IProfileReadOnly;
import com.castsoftware.dmt.engine.project.Project;

/**
 * Basic discoverer for
 */
public class JeeNetbeansProjectsDiscoverer extends BasicProjectsDiscovererAdapter
{
    /**
     * Default constructor used by the discovery engine
     */
    public JeeNetbeansProjectsDiscoverer()
    {
    }

    @Override
    public void buildProject(String relativeFilePath, String content, Project project,
        IProjectsDiscovererUtilities projectsDiscovererUtilities)
    {
        String projectDescriptor = project.getMetadata(IProfileReadOnly.METADATA_DESCRIPTOR).getValue();
        if ((!projectDescriptor.equals("project.xml")) || (!parseProjectFile(project, content, projectsDiscovererUtilities)))
            projectsDiscovererUtilities.deleteProject(project.getId());
    }

    private static boolean parseProjectFile(Project project, String content, IProjectsDiscovererUtilities projectsDiscovererUtilities)
    {
        Set<String> exports = ProjectFileScanner.scan(project, content, projectsDiscovererUtilities);
        if ((exports == null) || (exports.size() == 0))
            // no export = this project doesn't generate a jar
            return false;

        return true;
    }
}
