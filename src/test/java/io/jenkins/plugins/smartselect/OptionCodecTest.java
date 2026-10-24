package io.jenkins.plugins.smartselect;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class OptionCodecTest {
    @Test
    public void labelsAndValuesAreIndependent() {
        List<SelectOption> options =
                OptionCodec.parse(List.of(Map.of("label", "报价服务", "value", "price"), Map.of("value", "admin")));
        assertEquals("报价服务", options.get(0).getLabel());
        assertEquals("price", options.get(0).getValue());
        assertEquals("admin", options.get(1).getLabel());
    }

    @Test
    public void acceptsConfigurationJsonAndRoundtrips() {
        List<SelectOption> options = OptionCodec.parse("[{\"value\":\"price\",\"label\":\"报价 <服务>\"}]");
        assertEquals(options, OptionCodec.parse(OptionCodec.toJson(options)));
    }

    @Test
    public void rejectsAmbiguousOrInvalidValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> OptionCodec.parse(
                        List.of(Map.of("value", "price", "label", "报价"), Map.of("value", "price", "label", "其他"))));
        assertThrows(IllegalArgumentException.class, () -> OptionCodec.parse(List.of(Map.of("value", "a,b"))));
        assertThrows(IllegalArgumentException.class, () -> OptionCodec.parse(List.of(Map.of("label", "缺少值"))));
        assertThrows(IllegalArgumentException.class, () -> OptionCodec.parse(List.of(Map.of("value", 12))));
        assertThrows(IllegalArgumentException.class, () -> OptionCodec.parse("{\"value\":\"wrong-shape\"}"));
    }

    @Test
    public void parameterSubmitsValuesAndCopiesLabels() {
        SmartSelectParameterDefinition parameter = new SmartSelectParameterDefinition("SERVICES");
        parameter.setOptions(List.of(Map.of("value", "price", "label", "报价服务")));
        parameter.setDefaultValues("price");
        assertEquals("price", parameter.getDefaultParameterValue().getValue());
        assertEquals("price", parameter.createValue("price").getValue());
        assertThrows(IllegalArgumentException.class, () -> parameter.createValue("报价服务"));
        SmartSelectParameterDefinition copy =
                (SmartSelectParameterDefinition) parameter.copyWithDefaultValue(parameter.createValue("price"));
        assertEquals(parameter.getOptions(), copy.getOptions());
    }
}
