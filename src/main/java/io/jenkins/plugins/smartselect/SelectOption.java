package io.jenkins.plugins.smartselect;

import java.io.Serializable;
import java.util.Objects;

/** A display label paired with the stable value passed to builds. */
public final class SelectOption implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String value;
    private final String label;

    public SelectOption(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() { return value; }
    public String getLabel() { return label; }

    @Override public boolean equals(Object other) {
        return other instanceof SelectOption option
                && Objects.equals(value, option.value) && Objects.equals(label, option.label);
    }

    @Override public int hashCode() { return Objects.hash(value, label); }
}
