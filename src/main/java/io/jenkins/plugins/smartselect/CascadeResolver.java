package io.jenkins.plugins.smartselect;

import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.SimpleParameterDefinition;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolves dependency values from the submitted build and validates every parent before fetching. */
final class CascadeResolver {
    private CascadeResolver() {}

    static List<SelectOption> resolve(SmartSelectParameterDefinition parameter, Job<?, ?> job, Map<String, String> submitted) {
        return resolve(parameter, job, submitted, new HashSet<>());
    }

    private static List<SelectOption> resolve(SmartSelectParameterDefinition parameter, Job<?, ?> job,
                                              Map<String, String> submitted, Set<String> path) {
        if (parameter.getDependencies().isEmpty()) return parameter.getResolvedOptions();
        if (job == null || !parameter.isRemote()) throw new IllegalArgumentException("联动参数需要任务上下文和 REST 数据源");
        if (path.size() >= 16 || !path.add(parameter.getName())) throw new IllegalArgumentException("参数存在循环依赖或依赖层级过深");
        try {
            ParametersDefinitionProperty property = job.getProperty(ParametersDefinitionProperty.class);
            String url = parameter.getRestUrl();
            int queryStart = url.indexOf('?');
            for (String name : parameter.getDependencies()) {
                String token = "${" + name + "}";
                if (queryStart < 0 || url.indexOf(token) <= queryStart) throw new IllegalArgumentException("上级参数占位符只能用于 REST 查询参数");
                ParameterDefinition definition = property == null ? null : property.getParameterDefinition(name);
                String value;
                if (definition instanceof SmartSelectParameterDefinition parent) {
                    List<SelectOption> parentOptions = resolve(parent, job, submitted, path);
                    value = submitted.containsKey(name) ? submitted.get(name) : parent.defaultFor(parentOptions);
                    value = SelectionCodec.join(SelectionCodec.selectedFrom(value,
                            parentOptions.stream().map(SelectOption::getValue).toList(), parent.isMulti()));
                } else if (definition instanceof SimpleParameterDefinition simple) {
                    ParameterValue checked = submitted.containsKey(name) ? simple.createValue(submitted.get(name)) : simple.getDefaultParameterValue();
                    if (checked == null || !simple.isValid(checked)) throw new IllegalArgumentException("上级参数无效");
                    if (!(checked.getValue() instanceof String || checked.getValue() instanceof Boolean || checked.getValue() instanceof Number))
                        throw new IllegalArgumentException("不支持使用敏感或复杂参数作为上级参数");
                    value = String.valueOf(checked.getValue());
                    if (value.length() > 2048) throw new IllegalArgumentException("上级参数过长");
                } else throw new IllegalArgumentException("上级参数不存在或不支持联动");
                if (value == null || value.isBlank()) return List.of();
                url = url.replace(token, URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
            if (url.contains("${")) throw new IllegalArgumentException("REST 地址包含未声明的上级参数");
            return parameter.remoteOptions(url);
        } finally { path.remove(parameter.getName()); }
    }
}
