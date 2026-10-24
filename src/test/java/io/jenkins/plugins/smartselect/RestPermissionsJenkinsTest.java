package io.jenkins.plugins.smartselect;

import static org.junit.Assert.*;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.User;
import hudson.security.ACL;
import java.net.URL;
import java.util.List;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.util.NameValuePair;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import jenkins.model.Jenkins;
import org.springframework.security.access.AccessDeniedException;

public class RestPermissionsJenkinsTest {
    @Rule public JenkinsRule jenkins = new JenkinsRule();

    @Test public void onlyAdministratorsCanAuthorizeOriginsAndBuildersCanRefresh() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SmartSelectParameterDefinition parameter = new SmartSelectParameterDefinition("ENV");
        parameter.setChoices(List.of("dev"));
        project.addProperty(new ParametersDefinitionProperty(parameter));
        jenkins.jenkins.setSecurityRealm(jenkins.createDummySecurityRealm());
        jenkins.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ, Item.READ).everywhere().to("viewer")
                .grant(Jenkins.READ, Item.READ, Item.BUILD).everywhere().to("builder"));
        SmartSelectParameterDefinition.DescriptorImpl descriptor = jenkins.jenkins
                .getDescriptorByType(SmartSelectParameterDefinition.DescriptorImpl.class);
        try (var ignored = ACL.as2(User.getById("viewer", true).impersonate2())) {
            assertThrows(AccessDeniedException.class, () -> descriptor.setAllowedRestOrigins("https://example.com"));
        }
        for (String user : List.of("viewer", "builder")) try (JenkinsRule.WebClient client = jenkins.createWebClient().login(user)) {
            client.getOptions().setThrowExceptionOnFailingStatusCode(false);
            WebRequest request = new WebRequest(new URL(jenkins.getURL(), project.getUrl()
                    + "descriptorByName/io.jenkins.plugins.smartselect.SmartSelectParameterDefinition/options"), HttpMethod.POST);
            request.setRequestParameters(List.of(new NameValuePair("name", "ENV")));
            var response = client.getPage(client.addCrumb(request)).getWebResponse();
            assertEquals(user, user.equals("viewer") ? 403 : 200, response.getStatusCode());
            if (user.equals("builder")) assertTrue(response.getContentAsString().contains("dev"));
        }
    }
}
