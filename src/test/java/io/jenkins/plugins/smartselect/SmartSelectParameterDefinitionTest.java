package io.jenkins.plugins.smartselect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import hudson.model.StringParameterValue;
import java.util.List;
import net.sf.json.JSONObject;
import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest2;

public class SmartSelectParameterDefinitionTest {
    private SmartSelectParameterDefinition configured() {
        SmartSelectParameterDefinition parameter = new SmartSelectParameterDefinition("SERVICES");
        parameter.setChoices(List.of("price", "vehicle", "admin"));
        return parameter;
    }

    @Test public void defaultAndFormSubmission() {
        SmartSelectParameterDefinition parameter = configured();
        parameter.setDefaultValues(List.of("price", "vehicle"));
        assertEquals("price,vehicle", parameter.getDefaultParameterValue().getValue());
        assertEquals("vehicle,admin", ((StringParameterValue) parameter.createValue((StaplerRequest2) null,
                JSONObject.fromObject("{\"value\":\"vehicle,admin\"}"))).getValue());
    }

    @Test public void supportsSingleModeAndDefaultFirst() {
        SmartSelectParameterDefinition parameter = configured();
        parameter.setMode("SINGLE");
        assertEquals("price", parameter.getDefaultParameterValue().getValue());
        assertThrows(IllegalArgumentException.class, () -> parameter.createValue("price,vehicle"));
    }

    @Test public void rejectsTamperingAndTracksValidation() {
        SmartSelectParameterDefinition parameter = configured();
        assertTrue(parameter.isValid(new StringParameterValue("SERVICES", "price,vehicle")));
        assertFalse(parameter.isValid(new StringParameterValue("SERVICES", "unauthorized")));
        assertThrows(IllegalArgumentException.class, () -> parameter.createValue("unauthorized"));
    }

    @Test public void keepDefaultsOnCopyForRebuild() {
        SmartSelectParameterDefinition parameter = configured();
        SmartSelectParameterDefinition copy = (SmartSelectParameterDefinition)
                parameter.copyWithDefaultValue(new StringParameterValue("SERVICES", "vehicle,admin"));
        assertEquals("vehicle,admin", copy.getDefaultValueForBuild());
    }
}
