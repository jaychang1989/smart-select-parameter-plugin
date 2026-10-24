package io.jenkins.plugins.smartselect;

import static org.junit.Assert.*;
import com.sun.net.httpserver.HttpServer;
import hudson.cli.CLICommandInvoker;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.util.NameValuePair;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class CascadingJenkinsTest {
    @Rule public JenkinsRule jenkins = new JenkinsRule();

    @Test public void dependenciesUseActualSubmittedParentsAcrossApiFormAndCli() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String value = exchange.getRequestURI().getRawQuery().equals("env=release") ? "api-release" : "api-dev";
            byte[] bytes = ("[{\"value\":\"" + value + "\",\"label\":\"服务\"}]").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        try {
            String origin = "http://127.0.0.1:" + server.getAddress().getPort();
            jenkins.jenkins.getDescriptorByType(SmartSelectParameterDefinition.DescriptorImpl.class).setAllowedRestOrigins(origin);
            SmartSelectParameterDefinition parent = new SmartSelectParameterDefinition("ENV");
            parent.setMode("SINGLE"); parent.setChoices(List.of("dev", "release"));
            SmartSelectParameterDefinition child = new SmartSelectParameterDefinition("SERVICES");
            child.setMode("SINGLE"); child.setDependsOn("ENV"); child.setRestUrl(origin + "/?env=${ENV}");
            FreeStyleProject project = jenkins.createFreeStyleProject();
            project.addProperty(new ParametersDefinitionProperty(child, parent)); // independent of parameter order
            assertEquals("api-release", child.getOptionsFor(project, Map.of("ENV", "release")).get(0).getValue());
            assertThrows(IllegalArgumentException.class, () -> child.getOptionsFor(project, Map.of("ENV", "tampered")));
            try (JenkinsRule.WebClient client = jenkins.createWebClient()) {
                request(client, project, "buildWithParameters", List.of(
                        new NameValuePair("ENV", "release"), new NameValuePair("SERVICES", "api-release")));
                jenkins.waitUntilNoActivity();
                assertEquals(1, project.getLastBuild().getNumber());
                jenkins.assertBuildStatusSuccess(project.getLastBuild());
                client.getOptions().setThrowExceptionOnFailingStatusCode(false);
                int status = request(client, project, "buildWithParameters", List.of(
                        new NameValuePair("ENV", "dev"), new NameValuePair("SERVICES", "api-release")));
                assertTrue(status >= 400);
                jenkins.waitUntilNoActivity();
                assertEquals(1, project.getLastBuild().getNumber());
                client.getOptions().setThrowExceptionOnFailingStatusCode(true);
                request(client, project, "buildWithParameters", List.of(new NameValuePair("ENV", "dev")));
                jenkins.waitUntilNoActivity();
                assertEquals("api-dev", project.getLastBuild().getAction(ParametersAction.class).getParameter("SERVICES").getValue());
                request(client, project, "build", List.of(new NameValuePair("json",
                        "{\"parameter\":[{\"name\":\"SERVICES\",\"value\":\"api-release\"},{\"name\":\"ENV\",\"value\":\"release\"}]}")));
                jenkins.waitUntilNoActivity();
                assertEquals(3, project.getLastBuild().getNumber());
                assertEquals("api-release", project.getLastBuild().getAction(ParametersAction.class).getParameter("SERVICES").getValue());
            }
            CLICommandInvoker.Result cli = new CLICommandInvoker(jenkins, "build").invokeWithArgs(
                    project.getName(), "-p", "ENV=release", "-p", "SERVICES=api-release", "-s");
            assertEquals(cli.stderr(), 0, cli.returnCode());
            assertEquals(4, project.getLastBuild().getNumber());
            parent.setDependsOn("SERVICES"); parent.setRestUrl(origin + "/?service=${SERVICES}");
            assertThrows(IllegalArgumentException.class, () -> child.getOptionsFor(project, Map.of("ENV", "release")));
        } finally { server.stop(0); }
    }

    private int request(JenkinsRule.WebClient client, FreeStyleProject project, String action,
                        List<NameValuePair> values) throws Exception {
        WebRequest request = new WebRequest(new URL(jenkins.getURL(), project.getUrl() + action + "?delay=0sec"), HttpMethod.POST);
        request.setRequestParameters(values);
        return client.getPage(client.addCrumb(request)).getWebResponse().getStatusCode();
    }
}
