package io.jenkins.plugins.smartselect;

import static org.junit.Assert.*;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class UpgradeJenkinsTest {
    @Rule public JenkinsRule jenkins = new JenkinsRule();

    @Test public void loadsActualV01JobConfigurationAndBuildsWithoutNewSettings() throws Exception {
        FreeStyleProject project;
        try (var input = getClass().getResourceAsStream("legacy-v01.xml")) {
            assertNotNull(input);
            project = (FreeStyleProject) jenkins.jenkins.createProjectFromXML("legacy", input);
        }
        ParametersDefinitionProperty property = project.getProperty(ParametersDefinitionProperty.class);
        SmartSelectParameterDefinition services = (SmartSelectParameterDefinition) property.getParameterDefinition("SERVICES");
        assertEquals(List.of("insuredapi-price", "insuredapi-vehicle", "insuredapi-admin", "insuredapi-job"), services.getChoices());
        assertEquals("insuredapi-price", services.getDefaultParameterValue().getValue());
        assertTrue(services.getOptions().isEmpty());
        assertTrue(services.getDependsOn().isEmpty());
        assertFalse(services.isRemote());
        jenkins.configRoundtrip(project);
        var build = jenkins.buildAndAssertSuccess(project);
        assertEquals("insuredapi-price", build.getAction(ParametersAction.class).getParameter("SERVICES").getValue());
        assertEquals("pre-release", build.getAction(ParametersAction.class).getParameter("ENVIRONMENT").getValue());
    }
}
