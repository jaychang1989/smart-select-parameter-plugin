package io.jenkins.plugins.smartselect;

import static org.junit.Assert.*;
import com.sun.net.httpserver.HttpServer;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.htmlunit.WebRequest;
import org.htmlunit.HttpMethod;
import org.htmlunit.util.NameValuePair;

public class RestParameterJenkinsTest {
    @Rule public JenkinsRule jenkins = new JenkinsRule();

    @Test public void remoteLabelsCacheValidationAndEncryptedConfiguration() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/options", exchange -> {
            requests.incrementAndGet();
            byte[] body = "[{\"label\":\"报价服务\",\"value\":\"price\"}]".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            String origin = "http://127.0.0.1:" + server.getAddress().getPort();
            SmartSelectParameterDefinition.DescriptorImpl descriptor = jenkins.jenkins
                    .getDescriptorByType(SmartSelectParameterDefinition.DescriptorImpl.class);
            SmartSelectParameterDefinition parameter = new SmartSelectParameterDefinition("SERVICES");
            parameter.setRestUrl(origin + "/options");
            parameter.setRestToken("bearer-test-secret");
            parameter.setDefaultValues("price");
            assertThrows(IllegalArgumentException.class, parameter::getResolvedOptions);
            assertEquals(0, requests.get());
            descriptor.setAllowedRestOrigins(origin);
            descriptor.save();
            jenkins.configRoundtrip();
            assertEquals(origin, descriptor.getAllowedRestOrigins());
            assertEquals("报价服务", parameter.getResolvedOptions().get(0).getLabel());
            assertEquals("price", parameter.createValue("price").getValue());
            assertEquals(1, requests.get());
            assertThrows(IllegalArgumentException.class, () -> parameter.createValue("other"));
            FreeStyleProject project = jenkins.createFreeStyleProject();
            SmartSelectParameterDefinition labeled = new SmartSelectParameterDefinition("LABELLED");
            labeled.setOptions(List.of(Map.of("label", "管理服务", "value", "admin")));
            labeled.setDefaultValues("admin");
            project.addProperty(new ParametersDefinitionProperty(parameter, labeled));
            jenkins.configRoundtrip(project);
            assertFalse(project.getConfigFile().asString().contains("bearer-test-secret"));
            SmartSelectParameterDefinition saved = (SmartSelectParameterDefinition)
                    project.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("SERVICES");
            assertEquals(parameter.getRestUrl(), saved.getRestUrl());
            assertEquals("bearer-test-secret", saved.getRestToken().getPlainText());
            assertEquals("price", saved.getDefaultParameterValue().getValue());
            assertEquals(labeled, project.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("LABELLED"));
            try (JenkinsRule.WebClient client = jenkins.createWebClient()) {
                WebRequest request = new WebRequest(new URL(jenkins.getURL(), project.getUrl()
                        + "descriptorByName/io.jenkins.plugins.smartselect.SmartSelectParameterDefinition/options"), HttpMethod.POST);
                int beforeRefresh = requests.get();
                request.setRequestParameters(List.of(new NameValuePair("name", "SERVICES"), new NameValuePair("refresh", "true")));
                String json = client.getPage(client.addCrumb(request)).getWebResponse().getContentAsString();
                assertTrue(json.contains("报价服务"));
                assertFalse(json.contains("bearer-test-secret"));
                assertEquals("Manual refresh must bypass the cached response", beforeRefresh + 1, requests.get());
            }
            descriptor.setAllowedRestOrigins("");
            assertThrows(IllegalArgumentException.class, saved::getResolvedOptions);
        } finally { server.stop(0); }
    }

    @Test public void unavailableRemoteSourceNeverFallsBackToStaticChoices() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String origin = "http://127.0.0.1:" + server.getAddress().getPort();
        server.start(); server.stop(0);
        jenkins.jenkins.getDescriptorByType(SmartSelectParameterDefinition.DescriptorImpl.class).setAllowedRestOrigins(origin);
        SmartSelectParameterDefinition parameter = new SmartSelectParameterDefinition("SERVICES");
        parameter.setChoices(List.of("fallback"));
        parameter.setRestUrl(origin + "/options");
        parameter.setCacheSeconds(0);
        assertThrows(IllegalArgumentException.class, () -> parameter.createValue("fallback"));
        assertTrue(parameter.getView().getOptions().isEmpty());
        assertFalse(parameter.getView().getError().isEmpty());
    }
}
