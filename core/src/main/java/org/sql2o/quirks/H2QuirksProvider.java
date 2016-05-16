package org.sql2o.quirks;

/**
 * Created by lars on 10.08.15.
 */
public class H2QuirksProvider implements QuirksProvider {
    @Override
    public Quirks provide() {
        return new H2Quirks();
    }

    @Override
    public boolean isUsableForUrl(String url) {
        return url.startsWith("jdbc:h2");
    }

    @Override
    public boolean isUsableForClass(String className) {
        return className.startsWith("org.h2");
    }
}
