package io.jenkins.plugins.smartselect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import java.util.List;
import org.junit.Test;

public class SelectionCodecTest {
    @Test public void parseJenkinsfileList() {
        assertEquals(List.of("price", "vehicle"),
                SelectionCodec.choicesFrom(List.of("price", "vehicle", "price")));
    }

    @Test public void parseTextarea() {
        assertEquals(List.of("admin", "price"), SelectionCodec.choicesFrom("admin\n\n price\r\nadmin"));
    }

    @Test public void multiSelectValidatesAndDeduplicates() {
        assertEquals(List.of("vehicle", "price"),
                SelectionCodec.selectedFrom("vehicle,price,vehicle", List.of("price", "vehicle"), true));
    }

    @Test public void rejectSingleSelectWithTwoValues() {
        assertThrows(IllegalArgumentException.class,
                () -> SelectionCodec.selectedFrom("price,vehicle", List.of("price", "vehicle"), false));
    }

    @Test public void rejectUnknownInjectedValue() {
        assertThrows(IllegalArgumentException.class,
                () -> SelectionCodec.selectedFrom("price,delete-all", List.of("price"), true));
    }

    @Test public void rejectCommasInChoices() {
        assertThrows(IllegalArgumentException.class,
                () -> SelectionCodec.choicesFrom(List.of("a,b")));
    }
}
