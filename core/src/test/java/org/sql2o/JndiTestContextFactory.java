package org.sql2o;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A JNDI provider for the tests, so that the lookup in {@link JndiDataSource} can actually be exercised.
 *
 * <p>The context it hands out is a proxy rather than a stub class: only {@code lookup} and {@code close} are used by
 * sql2o, and every other method of the sizeable {@link Context} interface throws, which keeps this small.
 *
 * <p>JNDI instantiates the factory itself through {@link InitialContextFactory}, so the class has to be public with
 * a no-arg constructor, and the outcome of a lookup has to travel through static state.
 */
public class JndiTestContextFactory implements InitialContextFactory {

    /** What the next lookup returns. */
    static Object lookupResult;

    /** Whether the next lookup should fail instead. */
    static NamingException lookupFailure;

    /** How often the context was closed. */
    static final AtomicInteger closes = new AtomicInteger();

    /** Whether the context should fail to close. */
    static boolean closeFails;

    /** Whether the factory itself should fail, so that no context is ever created. */
    static boolean factoryFails;

    static void reset() {
        lookupResult = null;
        lookupFailure = null;
        closeFails = false;
        factoryFails = false;
        closes.set(0);
    }

    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        if (factoryFails) {
            throw new NamingException("this factory has nothing to offer");
        }
        return (Context) Proxy.newProxyInstance(
                JndiTestContextFactory.class.getClassLoader(),
                new Class<?>[] {Context.class},
                new ContextHandler());
    }

    private static class ContextHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "lookup":
                    if (lookupFailure != null) {
                        throw lookupFailure;
                    }
                    return lookupResult;
                case "close":
                    closes.incrementAndGet();
                    if (closeFails) {
                        throw new IllegalStateException("cannot close this context");
                    }
                    return null;
                case "toString":
                    return "JndiTestContext";
                default:
                    throw new UnsupportedOperationException(method.getName() + " is not needed for these tests");
            }
        }
    }
}