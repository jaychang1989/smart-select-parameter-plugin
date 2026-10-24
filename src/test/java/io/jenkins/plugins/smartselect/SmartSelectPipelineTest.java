package io.jenkins.plugins.smartselect;

import static org.junit.Assert.assertEquals;

import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterValue;
import java.util.List;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class SmartSelectPipelineTest {
    @Rule public JenkinsRule jenkins = new JenkinsRule();

    @Test public void declarativeParametersAcceptListsAndExposeSelectedValues() throws Exception {
        WorkflowJob project = jenkins.createProject(WorkflowJob.class, "smart-select-pipeline");
        String example = java.nio.file.Files.readString(java.nio.file.Path.of(
                System.getProperty("basedir", "."), "examples", "Jenkinsfile"), java.nio.charset.StandardCharsets.UTF_8);
        project.setDefinition(new CpsFlowDefinition(example, true));

        WorkflowRun defaults = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("SELECTED=price,vehicle;ENV=pre", defaults);
        jenkins.assertLogContains("LABELLED=admin", defaults);
        ParametersDefinitionProperty property = project.getProperty(ParametersDefinitionProperty.class);
        SmartSelectParameterDefinition parameter =
                (SmartSelectParameterDefinition) property.getParameterDefinition("SERVICES");
        assertEquals(List.of("price", "vehicle", "admin"), parameter.getChoices());
        assertEquals(List.of("price", "vehicle"), parameter.getDefaultValues());

        WorkflowRun selected = jenkins.assertBuildStatusSuccess(project.scheduleBuild2(0,
                new ParametersAction(new StringParameterValue("SERVICES", "admin"),
                        new StringParameterValue("ENV", "release"))));
        jenkins.assertLogContains("SELECTED=admin;ENV=release", selected);
    }
}
