package org.rrd4j.core;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

/**
 * Factory class which creates actual {@link RrdNioBackend} objects. This is the default factory since
 * 1.4.0 version
 */
public class RrdNioBackendFactory extends RrdFileBackendFactory {
    /**
     * Factory name, "NIO".
     */
    public static final String NAME = "NIO";

    /**
     * Period in seconds between consecutive synchronizations when
     * sync-mode is set to SYNC_BACKGROUND. By default in-memory cache will be
     * transferred to the disc every 300 seconds (5 minutes). Default value can be
     * changed via {@link #setSyncPeriod(int)} method.
     */
    public static final int DEFAULT_SYNC_PERIOD = 300; // seconds

    private static int syncPeriod = DEFAULT_SYNC_PERIOD;

    /**
     * The core pool size for the sync executor. Defaults to 6.
     */
    public static final int DEFAULT_SYNC_CORE_POOL_SIZE = 6;

    /**
     * The {@link ScheduledExecutorService} used to periodically sync the mapped file to disk with. 
     */
    private static ScheduledExecutorService syncExecutor =
            Executors.newScheduledThreadPool(DEFAULT_SYNC_CORE_POOL_SIZE);

    /**
     * Returns time between two consecutive background synchronizations. If not changed via
     * {@link #setSyncPeriod(int)} method call, defaults to {@link #DEFAULT_SYNC_PERIOD}.
     * See {@link #setSyncPeriod(int)} for more information.
     *
     * @return Time in seconds between consecutive background synchronizations.
     */
    public static int getSyncPeriod() {
        return syncPeriod;
    }

    /**
     * Sets time between consecutive background synchronizations.
     *
     * @param syncPeriod Time in seconds between consecutive background synchronizations.
     */
    public static void setSyncPeriod(int syncPeriod) {
        RrdNioBackendFactory.syncPeriod = syncPeriod;
    }

    /**
     * Creates RrdNioBackend object for the given file path.
     *
     * @param path     File path
     * @param readOnly True, if the file should be accessed in read/only mode.
     *                 False otherwise.
     * @return RrdNioBackend object which handles all I/O operations for the given file path
     * @throws IOException Thrown in case of I/O error.
     */
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
        return new RrdNioBackend(path, readOnly, syncExecutor, syncPeriod);
    }

    /**
     * Returns the name of this factory.
     *
     * @return Factory name (equals to string "NIO")
     */
	public String getFactoryName() {
		return NAME;    
	}
}
