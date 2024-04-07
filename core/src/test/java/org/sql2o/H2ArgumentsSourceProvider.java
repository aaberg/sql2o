package org.sql2o;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public class H2ArgumentsSourceProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        return Stream.of(
            Arguments.of("h2", "jdbc:h2:mem:test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1", "sa", "")
        );
    }
}
