package org.sql2o;

import org.sql2o.tools.SnakeToCamelCase;

public class NamingConvention {

    private final boolean caseSensitive;
    private final boolean autoDeriveColumnNames;

    public NamingConvention(boolean caseSensitive, boolean autoDeriveColumnNames) {
        this.caseSensitive = caseSensitive;
        this.autoDeriveColumnNames = autoDeriveColumnNames;
    }

    public String deriveName(String name) {
        var derivedName = name;

        if (autoDeriveColumnNames) {
            derivedName = SnakeToCamelCase.convert(derivedName);
        }
        if (!caseSensitive)
            derivedName = derivedName.toLowerCase();

        return derivedName;
    }
}
