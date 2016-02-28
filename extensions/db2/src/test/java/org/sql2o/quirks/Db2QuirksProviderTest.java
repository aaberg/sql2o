package org.sql2o.quirks;

import org.junit.Test;

import static org.junit.Assert.*;

public class Db2QuirksProviderTest {

    @Test
    public void providerWillReturnTheSameInstance(){
        Db2QuirksProvider provider = new Db2QuirksProvider();
        assertSame(provider.provide(), provider.provide());
    }

}
