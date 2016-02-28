package org.sql2o.quirks;

import org.junit.Test;

import static org.junit.Assert.*;

public class PostgresQuirksProviderTest {
    @Test
    public void providerWillReturnTheSameInstance(){
        PostgresQuirksProvider provider = new PostgresQuirksProvider();
        assertSame(provider.provide(), provider.provide());
    }
}
