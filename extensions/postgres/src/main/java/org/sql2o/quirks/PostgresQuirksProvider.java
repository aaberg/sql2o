package org.sql2o.quirks;

/**
 * Created by lars on 28.10.14.
 */
public class PostgresQuirksProvider implements QuirksProvider {


    @Override
    public Quirks provide() {
        return postgresQuirks.q;
    }

    @Override
    public boolean isUsableForUrl(String url) {
        return url.startsWith("jdbc:postgresql:");
    }

    @Override
    public boolean isUsableForClass(String className) {
        return className.startsWith("org.postgresql.");
    }

    private static class postgresQuirks {
        private static PostgresQuirks q = new PostgresQuirks();
    }
}
