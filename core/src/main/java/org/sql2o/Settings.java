package org.sql2o;

import org.sql2o.quirks.Quirks;

public class Settings {

    private final Quirks quirks;
    private final NamingConvention namingConvention;
    private final boolean throwOnMappingError;

    public Settings(NamingConvention namingConvention, Quirks quirks, boolean throwOnMappingError) {
        this.quirks = quirks;
        this.namingConvention = namingConvention;
        this.throwOnMappingError = throwOnMappingError;
    }

    public Quirks getQuirks() {
        return quirks;
    }

    public NamingConvention getNamingConvention() {
        return namingConvention;
    }

    public boolean isThrowOnMappingError(){
        return throwOnMappingError;
    }
}
