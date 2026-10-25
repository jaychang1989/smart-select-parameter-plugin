package io.jenkins.plugins.smartselect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.cli.CLICommandInvoker;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import java.net.URL;
import java.util.List;
import jenkins.model.Jenkins;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.util.NameValuePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class SmartSelectJenkinsTest {
    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule instance) {
        this.jenkins = instance;
    }

    private SmartSelectParameterDefinition parameter(String name, String mode) {
        SmartSelectParameterDefinition parameter = new SmartSelectParameterDefinition(name);
        parameter.setChoices(List.of("price", "vehicle", "admin"));
        parameter.setMode(mode);
        parameter.setDefaultValues("vehicle");
        parameter.setDescription("选择服务 <安全文本>");
        return parameter;
    }

    @Test
    public void choicesValidationRequiresProtectedConfigurationAccess() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        jenkins.jenkins.setSecurityRealm(jenkins.createDummySecurityRealm());
        jenkins.jenkins.setAuthorizationStrategy(new org.jvnet.hudson.test.MockAuthorizationStrategy()
                .grant(Jenkins.READ, Item.READ)
                .everywhere()
                .to("reader", "configurer")
                .grant(Item.CONFIGURE)
                .everywhere()
                .to("configurer"));
        String endpoint = project.getUrl()
                + "descriptorByName/io.jenkins.plugins.smartselect.SmartSelectParameterDefinition/checkChoices";
        try (JenkinsRule.WebClient client = jenkins.createWebClient().login("configurer")) {
            client.getOptions().setThrowExceptionOnFailingStatusCode(false);
            assertTrue(client.goTo(endpoint + "?value=one").getWebResponse().getStatusCode() >= 400);
            WebRequest request = new WebRequest(new URL(jenkins.getURL(), endpoint), HttpMethod.POST);
            request.setRequestParameters(List.of(new NameValuePair("value", "one")));
            assertEquals(403, client.getPage(request).getWebResponse().getStatusCode());
            assertEquals(
                    200,
                    client.getPage(client.addCrumb(request)).getWebResponse().getStatusCode());
        }
        try (JenkinsRule.WebClient client = jenkins.createWebClient().login("reader")) {
            client.getOptions().setThrowExceptionOnFailingStatusCode(false);
            WebRequest request = new WebRequest(new URL(jenkins.getURL(), endpoint), HttpMethod.POST);
            request.setRequestParameters(List.of(new NameValuePair("value", "one")));
            assertEquals(
                    403,
                    client.getPage(client.addCrumb(request)).getWebResponse().getStatusCode());
        }
    }

    @Test
    public void configurationSurvivesSaveAndReload() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SmartSelectParameterDefinition single = parameter("ENV", "SINGLE");
        single.setSearchable(false);
        SmartSelectParameterDefinition multi = parameter("SERVICES", "MULTI");
        multi.setDefaultValues("price,admin");
        project.addProperty(new ParametersDefinitionProperty(single, multi));

        jenkins.configRoundtrip(project);
        project = (FreeStyleProject) jenkins.jenkins.getItem(project.getName());
        project.doReload();
        ParametersDefinitionProperty saved = project.getProperty(ParametersDefinitionProperty.class);
        assertEquals(single, saved.getParameterDefinition("ENV"));
        assertEquals(multi, saved.getParameterDefinition("SERVICES"));
        assertFalse(((SmartSelectParameterDefinition) saved.getParameterDefinition("ENV")).isSearchable());
    }

    @Test
    public void buildFormSubmitsSelectedValues() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.addProperty(new ParametersDefinitionProperty(parameter("SERVICES", "MULTI")));
        try (JenkinsRule.WebClient client = jenkins.createWebClient().withJavaScriptEnabled(true)) {
            // Jenkins renders the parameter form with 405 because GET must not schedule a build.
            client.getOptions().setThrowExceptionOnFailingStatusCode(false);
            HtmlPage page = client.goTo(project.getUrl() + "build?delay=0sec");
            assertEquals(405, page.getWebResponse().getStatusCode());
            client.getOptions().setThrowExceptionOnFailingStatusCode(true);
            HtmlInput value = page.getFirstByXPath("//input[contains(@class, 'smart-select-value')]");
            assertNotNull(value);
            assertEquals("vehicle", value.getValue());
            HtmlButton trigger = page.getFirstByXPath("//button[contains(@class, 'smart-select-trigger')]");
            trigger.click();
            HtmlButton vehicle = page.getFirstByXPath("//button[@data-value='vehicle']");
            HtmlButton price = page.getFirstByXPath("//button[@data-value='price']");
            HtmlButton admin = page.getFirstByXPath("//button[@data-value='admin']");
            vehicle.click();
            price.click();
            admin.click();
            assertEquals("price,admin", value.getValue());
            HtmlForm form = page.getFormByName("parameters");
            jenkins.submit(form);
            jenkins.waitUntilNoActivity();
            assertNotNull(project.getLastBuild());
            jenkins.assertBuildStatusSuccess(project.getLastBuild());
            assertEquals(
                    "price,admin",
                    project.getLastBuild()
                            .getAction(ParametersAction.class)
                            .getParameter("SERVICES")
                            .getValue());
        }
    }

    @Test
    public void apiBuildAcceptsSelectionAndUsesMissingParameterDefault() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.addProperty(
                new ParametersDefinitionProperty(parameter("SERVICES", "MULTI"), parameter("ENV", "SINGLE")));
        try (JenkinsRule.WebClient client = jenkins.createWebClient()) {
            WebRequest request = new WebRequest(
                    new URL(jenkins.getURL(), project.getUrl() + "buildWithParameters?delay=0sec"), HttpMethod.POST);
            request.setRequestParameters(List.of(new NameValuePair("SERVICES", "price,admin")));
            client.getPage(client.addCrumb(request));
            jenkins.waitUntilNoActivity();
            assertNotNull(project.getLastBuild());
            jenkins.assertBuildStatusSuccess(project.getLastBuild());
            ParametersAction values = project.getLastBuild().getAction(ParametersAction.class);
            assertEquals("price,admin", values.getParameter("SERVICES").getValue());
            assertEquals("vehicle", values.getParameter("ENV").getValue());
        }
    }

    @Test
    public void apiRejectsUnknownChoicesAndMultipleSingleSelections() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.addProperty(new ParametersDefinitionProperty(parameter("ENV", "SINGLE")));
        try (JenkinsRule.WebClient client = jenkins.createWebClient()) {
            client.getOptions().setThrowExceptionOnFailingStatusCode(false);
            for (String submitted : List.of("unauthorized", "price,vehicle")) {
                WebRequest request = new WebRequest(
                        new URL(jenkins.getURL(), project.getUrl() + "buildWithParameters?delay=0sec"),
                        HttpMethod.POST);
                request.setRequestParameters(List.of(new NameValuePair("ENV", submitted)));
                int status = client.getPage(client.addCrumb(request))
                        .getWebResponse()
                        .getStatusCode();
                assertTrue(status >= 400, "Invalid selection must be rejected");
                jenkins.waitUntilNoActivity();
                assertNull(project.getLastBuild());
                assertTrue(jenkins.jenkins.getQueue().isEmpty());
            }
        }
    }

    @Test
    public void cliBuildPassesSelectionToBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.addProperty(new ParametersDefinitionProperty(parameter("SERVICES", "MULTI")));
        CLICommandInvoker.Result result = new CLICommandInvoker(jenkins, "build")
                .invokeWithArgs(project.getName(), "-p", "SERVICES=price,admin", "-s");
        assertEquals(0, result.returnCode(), result.stderr());
        jenkins.assertBuildStatusSuccess(project.getLastBuild());
        assertEquals(
                "price,admin",
                project.getLastBuild()
                        .getAction(ParametersAction.class)
                        .getParameter("SERVICES")
                        .getValue());
    }
}
