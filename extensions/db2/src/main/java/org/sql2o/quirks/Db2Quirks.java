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

import org.sql2o.converters.Converter;

import java.util.Map;

/**
 * @author aldenquimby@gmail.com
 * @since 4/6/14
 *
 * Db2 hands out DATE, TIME and TIMESTAMP columns as java.sql.Date, java.sql.Time and java.sql.Timestamp, which the
 * converters of core already read, so nothing has to be done about them here. That is measured by
 * {@code Db2DateReadingTest} against a real database rather than asserted in a comment.
 *
 * <p>Column naming is deliberately left to {@link NoQuirks}, which returns the label of a column and therefore the
 * alias a query gave it. This used to ask the metadata for the name of the column instead, which meant a query such
 * as {@code select id as my_id} would be mapped by {@code ID} and the value would silently never reach the property it
 * was written for.
 */
public class Db2Quirks extends NoQuirks {
    public Db2Quirks() {
        super();
    }

    public Db2Quirks(Map<Class, Converter> converters) {
        super(converters);
    }
}
