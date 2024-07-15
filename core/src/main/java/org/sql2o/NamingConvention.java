package org.sql2o;

public class NamingConvention {

    private final boolean caseSensitive;

    public NamingConvention(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public String deriveName(String name) {
        if (!caseSensitive)
            return name.toLowerCase();

        return name;
    }
}
