package org.sql2o;

import org.sql2o.tools.SnakeToCamelCase;

import java.util.Objects;

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

    /**
     * Two conventions are equal when they derive names in the same way, which makes them interchangeable,
     * for instance as part of a cache key.
     *
     * <p>Equality is class based on purpose: a subclass that adds state has to override equals and hashCode,
     * otherwise it would compare equal to a plain NamingConvention carrying the same flags.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final var other = (NamingConvention) o;
        return caseSensitive == other.caseSensitive && autoDeriveColumnNames == other.autoDeriveColumnNames;
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseSensitive, autoDeriveColumnNames);
    }
}
