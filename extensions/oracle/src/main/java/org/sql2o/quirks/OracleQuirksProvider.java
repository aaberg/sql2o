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

/**
 * Created by lars on 28.10.14.
 */
public class OracleQuirksProvider implements QuirksProvider {
    @Override
    public Quirks provide() {
        return oracleQuirks.q;
    }

    @Override
    public boolean isUsableForUrl(String url) {
        return url.startsWith("jdbc:oracle:");
    }

    @Override
    public boolean isUsableForClass(String className) {
        return className.startsWith("oracle.jdbc.")
                || className.startsWith("oracle.jdbc.");
    }

    private static class oracleQuirks {
        private static final OracleQuirks q = new OracleQuirks();
    }

}
