package org.sql2o;

import org.sql2o.quirks.Quirks;

public class Settings {

    private final Quirks quirks;
    private final NamingConvention namingConvention;

    public Settings(NamingConvention namingConvention, Quirks quirks) {
        this.quirks = quirks;
        this.namingConvention = namingConvention;
    }

    public Quirks getQuirks() {
        return quirks;
    }

    public NamingConvention getNamingConvention() {
        return namingConvention;
    }
}
