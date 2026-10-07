package org.sql2o.converters;

import java.util.Map;

/**
 * Registers one extra converter through the {@link ConvertersProvider} service interface, so that the extension
 * point is actually exercised by the tests. Registered for {@link Money}, a type nothing else in the suite looks
 * up, which keeps it from changing the behaviour of other tests.
 */
public class MoneyConverterProvider implements ConvertersProvider {

    /**
     * A stand-in for a domain type that an application would want to read straight out of a query.
     */
    public static final class Money {

        private final long cents;

        public Money(long cents) {
            this.cents = cents;
        }

        public long getCents() {
            return cents;
        }
    }

    @Override
    public void fill(Map<Class<?>, Converter<?>> converters) {
        converters.put(Money.class, new ConverterBase<Money>() {
            @Override
            public Money convert(Object val) throws ConverterException {
                if (val == null) {
                    return null;
                }
                if (val instanceof Money) {
                    return (Money) val;
                }
                if (val instanceof Number) {
                    return new Money(((Number) val).longValue());
                }
                throw new ConverterException("Cannot convert " + val.getClass().getName() + " to Money");
            }
        });
    }
}