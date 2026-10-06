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
 * How {@link QuirksDetector} finds {@link MysqlQuirks} when nobody passes quirks explicitly.
 *
 * <p>Only the urls and the classes of the MySQL driver itself are claimed: a {@code jdbc:mariadb:} url belongs to
 * the MariaDB driver, whose classes differ, even though the server the tests run against is MariaDB and answers
 * either driver over the same wire protocol.
 */
public class MysqlQuirksProvider implements QuirksProvider {
    @Override
    public Quirks provide() {
        return new MysqlQuirks();
    }

    @Override
    public boolean isUsableForUrl(String url) {
        return url.startsWith("jdbc:mysql:");
    }

    @Override
    public boolean isUsableForClass(String className) {
        return className.startsWith("com.mysql.cj.jdbc");
    }
}
