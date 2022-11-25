package org.sql2o.quirks;

/**
 * Created by davidlzs on 25.11.22.
 */
public class MySQLQuirksProvider implements QuirksProvider {


    @Override
    public Quirks provide() {
        return new MySQLQuirks();
    }

    @Override
    public boolean isUsableForUrl(String url) {
        return url.startsWith("jdbc:mysql:");
    }

    @Override
    public boolean isUsableForClass(String className) {
        return className.startsWith("com.mysql.");
    }
}
