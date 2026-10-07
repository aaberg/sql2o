package org.sql2o;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runs the annotated test method once for every database returned by {@link TestDatabase#databases()}.
 *
 * The test class only needs a {@code static Stream<TestDatabase> databases()} method returning
 * {@code TestDatabase.databases()}, and the test method takes a single {@link TestDatabase} argument.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ParameterizedTest(name = "{0}")
@MethodSource("databases")
public @interface DatabaseTest {
}