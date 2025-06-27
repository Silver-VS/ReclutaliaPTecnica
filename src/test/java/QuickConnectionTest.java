/*
 * QuickConnectionTest.java
 * ---------------------------------------------------------------------------
 * Copyright © 2025 Silver VS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 * ---------------------------------------------------------------------------
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import config.DatabaseConfig;

/**
 * <h2>QuickConnectionTest</h2>
 *
 * <p>Small command-line utility to verify at runtime that:</p>
 * <ul>
 *   <li>environment variables or AWS Secrets Manager entries are correctly read by {@link config.DatabaseConfig},</li>
 *   <li>the HikariCP connection pool is initialized and can provide valid connections,</li>
 *   <li>the database endpoint is reachable from the executing host.</li>
 * </ul>
 *
 * <p>Outputs an INFO log with the connection URL on success or an ERROR log
 * with the exception stack trace on failure.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * mvn -q exec:java -Dexec.mainClass="com.example.QuickConnectionTest"
 * }</pre>
 *
 * @author Silver VS
 * @version 1.0.0 · 26 jun 2025
 * @since 0.1.0
 */
public final class QuickConnectionTest {

    /** SLF4J logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(QuickConnectionTest.class);

    /**
     * Entry point for the quick connection test utility.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(final String[] args) {
        try (Connection con = DatabaseConfig.getDataSource().getConnection()) {
            LOG.info("✔ Connection successful to: {}", con.getMetaData().getURL());
        } catch (Exception e) {
            LOG.error("✖ Connection error: {}", e.getMessage(), e);
        }
    }

    /** Prevent instantiation of this utility class. */
    private QuickConnectionTest() {
        throw new AssertionError("Utility class—do not instantiate.");
    }
}
