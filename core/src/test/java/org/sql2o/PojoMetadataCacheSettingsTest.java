package org.sql2o;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sql2o.converters.Converter;
import org.sql2o.quirks.NoQuirks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The POJO metadata cache in {@link org.sql2o.reflection2.ObjectBuildableFactory} is keyed by class only, while
 * the metadata it holds is derived from the {@link Settings} of whichever instance happened to map the class
 * first. These tests pin the expected behaviour: metadata must not leak from one {@link Sql2o} instance to
 * another.
 *
 * <p>Both tests use a POJO class of their own. The cache is a process wide static map without eviction, so a
 * POJO class that is used by more than one test would make the outcome depend on test execution order.
 *
 * <p>Expected to fail until the cache key takes the relevant settings into account.
 */
public class PojoMetadataCacheSettingsTest {

    private static final String URL = "jdbc:h2:mem:pomdmetadatacachesettings;DB_CLOSE_DELAY=-1";

    private Sql2o caseInsensitive;
    private Sql2o caseSensitive;

    @BeforeEach
    void setUp() {
        caseInsensitive = new Sql2o(URL, "sa", "");
        caseSensitive = new Sql2o(URL, "sa", "");
        caseSensitive.setDefaultCaseSensitive(true);

        CountingConverter.CALLS.set(0);
    }

    /**
     * The field is deliberately named in upper case, so that it matches the column under either naming
     * convention. With a lower case field the case sensitive instance could not map the column even with a
     * correct cache, and the test would prove nothing.
     */
    public static class CaseSensitivePojo {
        public int ID;
    }

    public static class DomainValue {
        public final String raw;

        public DomainValue(String raw) {
            this.raw = raw;
        }
    }

    /**
     * Counts how often it is asked to convert, so that a leaked converter is visible.
     */
    public static class CountingConverter implements Converter<DomainValue> {

        static final AtomicInteger CALLS = new AtomicInteger();

        @Override
        public DomainValue convert(Object val) {
            CALLS.incrementAndGet();
            return new DomainValue(String.valueOf(val));
        }

        @Override
        public Object toDatabaseParam(DomainValue val) {
            return val.raw;
        }
    }

    public static class QuirksPojo {
        public DomainValue value;
    }

    @Test
    public void caseSensitiveInstance_isNotAffectedByMetadataOfAnotherInstance() {
        // Whichever instance maps the class first derives the property names, and that result is cached.
        try (Connection con = caseInsensitive.open()) {
            CaseSensitivePojo pojo = con.createQuery("select 1 as \"ID\" from (values(0))")
                    .executeAndFetchFirst(CaseSensitivePojo.class);

            assertEquals(1, pojo.ID);
        }

        // This instance derives property names case sensitively, so it needs its own metadata. Reusing the
        // cached one leaves the property under the key "ID" while the column is looked up as "ID" against a
        // "id" keyed property, and the mapping fails with "Could not map ID to any property.".
        try (Connection con = caseSensitive.open()) {
            CaseSensitivePojo pojo = con.createQuery("select 1 as \"ID\" from (values(0))")
                    .executeAndFetchFirst(CaseSensitivePojo.class);

            assertEquals(1, pojo.ID);
        }
    }

    @Test
    public void instanceWithoutCustomConverters_doesNotInheritThemFromAnotherInstance() {
        Map<Class, Converter> converters = new HashMap<>();
        converters.put(DomainValue.class, new CountingConverter());
        Sql2o withConverter = new Sql2o(URL, "sa", "", new NoQuirks(converters));
        Sql2o withoutConverter = new Sql2o(URL, "sa", "");

        try (Connection con = withConverter.open()) {
            QuirksPojo pojo = con.createQuery("select 'abc' as \"VALUE\" from (values(0))")
                    .executeAndFetchFirst(QuirksPojo.class);

            assertEquals(1, CountingConverter.CALLS.get());
            assertEquals("abc", pojo.value.raw);
        }

        // The second instance has no converter for DomainValue and falls back to the DefaultConverter, which
        // passes the raw value through, so the mapping cannot succeed. The point is that it must not succeed
        // by using the converter that was registered on the other instance.
        CountingConverter.CALLS.set(0);
        try (Connection con = withoutConverter.open()) {
            assertThrows(RuntimeException.class, () ->
                    con.createQuery("select 'abc' as \"VALUE\" from (values(0))")
                            .executeAndFetchFirst(QuirksPojo.class));

            assertEquals(0, CountingConverter.CALLS.get());
        }
    }
}