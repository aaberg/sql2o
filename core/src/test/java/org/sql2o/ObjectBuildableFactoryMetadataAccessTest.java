package org.sql2o;

import org.junit.jupiter.api.Test;
import org.sql2o.converters.Converter;
import org.sql2o.quirks.NoQuirks;
import org.sql2o.quirks.Quirks;
import org.sql2o.reflection2.ObjectBuildableFactory;
import org.sql2o.reflection2.PojoMetadata;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The cached metadata of {@link ObjectBuildableFactory} is public, and the tests sit in this package on purpose.
 *
 * <p>That is the whole point of the placement: {@code pojoMetadata} lives in {@code org.sql2o.reflection2}, so from
 * here it would not compile if it were package-private, which makes this file the guard against the visibility being
 * narrowed again. Nothing else here would notice. The bytecode extension in {@code extensions/bytecode} needs the
 * accessor precisely because it is written outside that package, and would otherwise have to introspect every class
 * again on every row.
 *
 * <p>The cache is a process wide static map without eviction, so the POJO classes here are of their own and are not
 * shared with any other test. That is the same reason {@code PojoMetadataCacheSettingsTest} gives for its own.
 */
public class ObjectBuildableFactoryMetadataAccessTest {

    public static class Pojo {
        public int id;
    }

    public static class AnotherPojo {
        public int id;
    }

    private static Settings settings(boolean caseSensitive, boolean autoDerive, Quirks quirks) {
        return new Settings(new NamingConvention(caseSensitive, autoDerive), quirks, true);
    }

    private static Settings settings() {
        return settings(false, false, new NoQuirks());
    }

    @Test
    public void theMetadataOfAClassIsHandedOutFromTheCacheRatherThanBuiltAgain() {
        final Settings settings = settings();

        final PojoMetadata<Pojo> first = ObjectBuildableFactory.pojoMetadata(Pojo.class, settings);
        final PojoMetadata<Pojo> second = ObjectBuildableFactory.pojoMetadata(Pojo.class, settings);

        assertSame(first, second);
    }

    /**
     * The naming convention is part of the key rather than an accident of ordering, since the metadata derives its
     * property names from it and metadata built under one convention means nothing under another.
     */
    @Test
    public void eachNamingConventionHasMetadataOfItsOwn() {
        final PojoMetadata<Pojo> caseInsensitive =
                ObjectBuildableFactory.pojoMetadata(Pojo.class, settings(false, false, new NoQuirks()));
        final PojoMetadata<Pojo> caseSensitive =
                ObjectBuildableFactory.pojoMetadata(Pojo.class, settings(true, false, new NoQuirks()));

        assertNotSame(caseInsensitive, caseSensitive);
    }

    /** And two instances that happen to be equal are the same key, which is what the record key of the cache relies on. */
    @Test
    public void twoEqualSettingsAreTheSameKeyRatherThanTwoEntries() {
        final PojoMetadata<Pojo> first =
                ObjectBuildableFactory.pojoMetadata(Pojo.class, settings(false, false, new NoQuirks()));
        final PojoMetadata<Pojo> second =
                ObjectBuildableFactory.pojoMetadata(Pojo.class, settings(false, false, new NoQuirks()));

        assertSame(first, second);
    }

    /**
     * The quirks are deliberately not part of the key, since nothing in the metadata uses them: they are handed to
     * {@code PojoProperty.SetProperty} by whoever is writing a value. A caller passing its own converter map therefore
     * still gets the shared metadata rather than a second copy built for its own settings.
     */
    @Test
    public void theQuirksAreNotPartOfTheKey() {
        final Map<Class, Converter> converters = new HashMap<>();
        converters.put(String.class, new org.sql2o.converters.DefaultConverter());

        final PojoMetadata<Pojo> withPlainQuirks =
                ObjectBuildableFactory.pojoMetadata(Pojo.class, settings(false, false, new NoQuirks()));
        final PojoMetadata<Pojo> withOwnQuirks =
                ObjectBuildableFactory.pojoMetadata(Pojo.class, settings(false, false, new NoQuirks(converters)));

        assertSame(withPlainQuirks, withOwnQuirks);
    }

    /** Each class has its own metadata, or two unrelated classes would map onto one another's properties. */
    @Test
    public void eachClassHasMetadataOfItsOwn() {
        final Settings settings = settings();

        assertNotSame(
                ObjectBuildableFactory.pojoMetadata(Pojo.class, settings),
                ObjectBuildableFactory.pojoMetadata(AnotherPojo.class, settings));
    }
}