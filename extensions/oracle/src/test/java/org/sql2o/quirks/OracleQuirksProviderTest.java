package org.sql2o.quirks;

import org.junit.Test;

import static org.junit.Assert.*;

public class OracleQuirksProviderTest {
    @Test
    public void providerWillReturnTheSameInstance(){
        OracleQuirksProvider provider = new OracleQuirksProvider();
        assertSame(provider.provide(), provider.provide());
    }
}
