/*
 * Copyright (c) 2026 Dmitry Alexandrov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.sql2o.quirks;

/**
 * How {@link QuirksDetector} finds {@link DerbyQuirks} when nobody passes quirks explicitly.
 */
public class DerbyQuirksProvider implements QuirksProvider {
    @Override
    public Quirks provide() {
        return new DerbyQuirks();
    }

    @Override
    public boolean isUsableForUrl(String url) {
        return url.startsWith("jdbc:derby:");
    }

    @Override
    public boolean isUsableForClass(String className) {
        return className.startsWith("org.apache.derby.jdbc")
                // The client driver sits beside the embedded one and speaks to the same database, so a caller using
                // the network driver gets the quirks rather than being left with the defaults.
                || className.startsWith("org.apache.derby.client");
    }
}