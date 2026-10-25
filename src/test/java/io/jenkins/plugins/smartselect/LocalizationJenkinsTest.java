package io.jenkins.plugins.smartselect;

import static org.junit.jupiter.api.Assertions.*;

import hudson.Functions;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.util.NameValuePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.localizer.LocaleProvider;

@WithJenkins
class LocalizationJenkinsTest {
    private LocaleProvider previousProvider;

    @BeforeEach
    void useRequestLocale() {
        // JenkinsRule bypasses WebAppMain, which installs this provider in a real controller.
        previousProvider = LocaleProvider.getProvider();
        LocaleProvider.setProvider(new LocaleProvider() {
            @Override
            public Locale get() {
                return Functions.getCurrentLocale();
            }
        });
    }

    @AfterEach
    void restoreLocaleProvider() {
        LocaleProvider.setProvider(previousProvider);
    }

    @Test
    void englishDefaultAndChineseFollowRequestLanguage(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SmartSelectParameterDefinition parameter = new SmartSelectParameterDefinition("SERVICES");
        parameter.setChoices(
                IntStream.range(0, 101).mapToObj(i -> "service-" + i).toList());
        SmartSelectParameterDefinition remote = new SmartSelectParameterDefinition("REMOTE");
        remote.setRestUrl("http://127.0.0.1:1/options"); // Unapproved origin: no network request is made.
        project.addProperty(new ParametersDefinitionProperty(parameter, remote));
        for (String language : List.of("en-US", "fr-FR", "zh-CN")) {
            boolean chinese = language.equals("zh-CN");
            try (JenkinsRule.WebClient client = jenkins.createWebClient().withJavaScriptEnabled(true)) {
                client.addRequestHeader("Accept-Language", language);
                client.getOptions().setThrowExceptionOnFailingStatusCode(false);
                HtmlPage page = client.goTo(project.getUrl() + "build?delay=0sec");
                assertEquals(405, page.getWebResponse().getStatusCode());
                HtmlButton trigger = page.getFirstByXPath("//button[contains(@class,'smart-select-trigger')]");
                assertEquals(
                        chinese ? "请选择" : "Select...",
                        trigger.asNormalizedText().replace("⌄", "").trim());
                assertTrue(page.asNormalizedText().contains(chinese ? "数据源或默认值" : "data source or default values"));
                if (!chinese) assertFalse(page.asNormalizedText().matches("(?s).*\\p{IsHan}.*"));
                trigger.click();
                assertTrue(page.asNormalizedText().contains(chinese ? "第 1/2 页" : "Page 1/2"));
                assertTrue(page.asNormalizedText().contains(chinese ? "全选结果" : "Select all results"));
                HtmlButton selectAll = page.getFirstByXPath("//button[contains(@class,'smart-select-select-all')]");
                selectAll.click();
                assertTrue(trigger.asNormalizedText().contains(chinese ? "已选择 101 项" : "101 selected"));
                assertTrue(page.asNormalizedText().contains(chinese ? "已选 101 / 共 101 项" : "101 / 101 selected"));
                HtmlButton refresh = page.getFirstByXPath("//button[contains(@class,'smart-select-refresh')]");
                refresh.click();
                client.waitForBackgroundJavaScript(3000);
                assertTrue(page.asNormalizedText()
                        .contains(chinese ? "请检查数据源配置后重试" : "Check the data source configuration and try again."));
                String descriptor = project.getUrl()
                        + "descriptorByName/io.jenkins.plugins.smartselect.SmartSelectParameterDefinition/";
                WebRequest validation =
                        new WebRequest(new URL(jenkins.getURL(), descriptor + "checkChoices"), HttpMethod.POST);
                validation.setRequestParameters(List.of(new NameValuePair("value", "")));
                assertTrue(client.getPage(client.addCrumb(validation))
                        .getWebResponse()
                        .getContentAsString()
                        .contains(chinese ? "请至少配置一个选项" : "Configure at least one choice"));
                WebRequest options = new WebRequest(new URL(jenkins.getURL(), descriptor + "options"), HttpMethod.POST);
                options.setRequestParameters(List.of(new NameValuePair("name", "REMOTE")));
                assertTrue(client.getPage(client.addCrumb(options))
                        .getWebResponse()
                        .getContentAsString()
                        .contains(chinese ? "请检查数据源配置后重试" : "Check the data source configuration and try again."));
                HtmlPage config = client.goTo(project.getUrl() + "configure");
                assertTrue(config.asNormalizedText().contains(chinese ? "缓存秒数" : "Cache seconds"));
                assertTrue(config.asNormalizedText().contains(chinese ? "上级参数" : "Parent parameters"));
                if (!chinese) assertFalse(config.asNormalizedText().matches("(?s).*\\p{IsHan}.*"));
                HtmlPage system = client.goTo("configure");
                assertTrue(system.asNormalizedText()
                        .contains(chinese ? "Smart Select REST 数据源授权" : "Smart Select REST source authorization"));
                if (!chinese) assertFalse(system.asNormalizedText().matches("(?s).*\\p{IsHan}.*"));
                assertNull(project.getLastBuild());
            }
        }
    }
}
