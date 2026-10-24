package io.jenkins.plugins.smartselect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public final class OptionCodec {
    private OptionCodec() {}

    public static List<SelectOption> parse(Object input) {
        if (input == null || input instanceof String text && text.isBlank()) return List.of();
        Object decoded = input;
        if (input instanceof String text) {
            if (!text.stripLeading().startsWith("[")) throw new IllegalArgumentException("Options must be a JSON array");
            try { decoded = JSONArray.fromObject(text); }
            catch (RuntimeException e) { throw new IllegalArgumentException("Invalid options JSON"); }
        }
        if (!(decoded instanceof Collection<?> entries)) throw new IllegalArgumentException("Options must be a list");
        Map<String, SelectOption> distinct = new LinkedHashMap<>();
        for (Object entry : entries) {
            String value;
            String label;
            if (entry instanceof String text) {
                value = text;
                label = text;
            } else if (entry instanceof Map<?, ?> map && map.get("value") instanceof String text) {
                value = text;
                Object rawLabel = map.get("label");
                if (rawLabel != null && !(rawLabel instanceof String)) throw new IllegalArgumentException("Label must be a string");
                label = rawLabel == null ? value : (String) rawLabel;
            } else throw new IllegalArgumentException("Each option needs a string value");
            List<String> checked = SelectionCodec.choicesFrom(List.of(value));
            if (checked.size() != 1) throw new IllegalArgumentException("Option value must not be empty");
            value = checked.get(0);
            label = label.trim();
            if (label.isEmpty() || label.length() > 256 || label.indexOf('\n') >= 0 || label.indexOf('\r') >= 0)
                throw new IllegalArgumentException("Label must contain 1 to 256 characters without line breaks");
            SelectOption option = new SelectOption(value, label);
            SelectOption previous = distinct.putIfAbsent(value, option);
            if (previous != null && !previous.equals(option)) throw new IllegalArgumentException("Duplicate option value has different labels");
            if (distinct.size() > 2000) throw new IllegalArgumentException("At most 2000 options are supported");
        }
        return List.copyOf(distinct.values());
    }

    public static String toJson(List<SelectOption> options) {
        JSONArray result = new JSONArray();
        for (SelectOption option : options) {
            JSONObject item = new JSONObject();
            item.put("value", option.getValue());
            item.put("label", option.getLabel());
            result.add(item);
        }
        return result.toString();
    }
}
