/*
 * Copyright (c) 2014 Lars Aaberg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.sql2o.quirks;

import org.sql2o.GenericDatasource;

import javax.sql.DataSource;
import javax.xml.ws.Service;
import java.util.ServiceLoader;

/**
 * Automatically detects which quirks implementation to use. Falls back on NoQuirks.
 */
public class QuirksDetector{
    static final ServiceLoader<QuirksProvider> providers = ServiceLoader.load(QuirksProvider.class);

    public static Quirks forURL(String jdbcUrl) {

        for (QuirksProvider quirksProvider : ServiceLoader.load(QuirksProvider.class)) {
            if (quirksProvider.isUsableForUrl(jdbcUrl)){
                return quirksProvider.provide();
            }
        }

        return new NoQuirks();
    }

    public static Quirks forObject(Object jdbcObject) {

        String jdbcObjectClassName = jdbcObject.getClass().getName().contains("$") ?
                jdbcObject.getClass().getSuperclass().getCanonicalName() :
                jdbcObject.getClass().getCanonicalName();

        for (QuirksProvider quirksProvider : ServiceLoader.load(QuirksProvider.class)) {
            if (quirksProvider.isUsableForClass(jdbcObjectClassName)){
                return quirksProvider.provide();
            }
        }

        return new NoQuirks();
    }
}
