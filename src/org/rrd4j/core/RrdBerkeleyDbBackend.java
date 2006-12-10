package org.rrd4j.core;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Database;

import java.io.IOException;

/**
 * Backend which is used to store RRD data to ordinary disk files
 * using <a href="http://www.sleepycat.com/">Sleepycat Berkeley DB</a>.
 *
 * @author <a href="mailto:m.bogaert@memenco.com">Mathias Bogaert</a>
 */
public class RrdBerkeleyDbBackend extends RrdBackend {
    private byte[] buffer;
    private Database rrdDatabase;

    public RrdBerkeleyDbBackend(String path, Database rrdDatabase) {
        super(path);
        this.buffer = new byte[0];
        this.rrdDatabase = rrdDatabase;
    }

    public RrdBerkeleyDbBackend(byte[] buffer, String path, Database rrdDatabase) {
        super(path);
        this.buffer = buffer;
        this.rrdDatabase = rrdDatabase;
    }

    protected synchronized void write(long offset, byte[] bytes) {
        int pos = (int) offset;
        for (byte a : bytes) {
            buffer[pos++] = a;
        }
    }

    protected synchronized void read(long offset, byte[] b) {
        int pos = (int) offset;
        for (int i = 0; i < b.length; i++) {
            b[i] = buffer[pos++];
        }
    }

    public long getLength() {
        return buffer.length;
    }

    protected void setLength(long length) throws IOException {
        if (length > Integer.MAX_VALUE) {
            throw new IOException("Cannot create this big Berkeley DB backed RRD: " + length);
        }

        buffer = new byte[(int) length];
    }

    public void close() throws IOException {
        DatabaseEntry theKey = new DatabaseEntry(getPath().getBytes("UTF-8"));
        DatabaseEntry theData = new DatabaseEntry(buffer);

        try {
            // because the database was opened to support transactions, this write is performed
            // using auto commit
            rrdDatabase.put(null, theKey, theData);
        }
        catch (DatabaseException de) {
            throw new IOException(de.getMessage());
        }
    }

    /**
     * This method is overriden to disable high-level caching in frontend Rrd4j classes.
     *
     * @return Always returns <code>false</code>. There is no need to cache anything in high-level classes
     *         since all RRD bytes are already in memory.
     */
    protected boolean isCachingAllowed() {
        return false;
    }
}
