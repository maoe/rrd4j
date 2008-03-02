/* ============================================================
 * Rrd4j : Pure java implementation of RRDTool's functionality
 * ============================================================
 *
 * Project Info:  http://www.rrd4j.org
 * Project Lead:  Mathias Bogaert (m.bogaert@memenco.com)
 *
 * (C) Copyright 2003-2007, by Sasa Markovic.
 *
 * Developers:    Sasa Markovic
 *
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package org.rrd4j.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class should be used to synchronize access to RRD files
 * in a multithreaded environment. This class should be also used to prevent opening of
 * too many RRD files at the same time (thus avoiding operating system limits).
 */
public class RrdDbPoolOld extends RrdDbPool {

	private int capacity = INITIAL_CAPACITY;
	private Map<String, RrdEntry> rrdMap = new HashMap<String, RrdEntry>(INITIAL_CAPACITY);

	private RrdDbPoolOld() {
		RrdBackendFactory factory = RrdBackendFactory.getDefaultFactory();

        if (!(factory instanceof RrdFileBackendFactory)) {
			throw new RuntimeException("Cannot create instance of " + getClass().getName() + " with " +
					"a default backend factory not derived from RrdFileBackendFactory");
		}
	}

	/* (non-Javadoc)
	 * @see org.rrd4j.core.RrdDbPoolI#requestRrdDb(java.lang.String)
	 */
	public synchronized RrdDb requestRrdDb(String path) throws IOException {
		String canonicalPath = Util.getCanonicalPath(path);
		while (!rrdMap.containsKey(canonicalPath) && rrdMap.size() >= capacity) {
			try {
				wait();
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		if (rrdMap.containsKey(canonicalPath)) {
			// already open, just increase usage count
			RrdEntry entry = rrdMap.get(canonicalPath);
			entry.count++;
			return entry.rrdDb;
		}
		else {
			// not open, open it now and add to the map
			RrdDb rrdDb = new RrdDb(canonicalPath);
			rrdMap.put(canonicalPath, new RrdEntry(rrdDb));
			return rrdDb;
		}
	}

	/* (non-Javadoc)
	 * @see org.rrd4j.core.RrdDbPoolI#requestRrdDb(org.rrd4j.core.RrdDef)
	 */
	public synchronized RrdDb requestRrdDb(RrdDef rrdDef) throws IOException {
		String canonicalPath = Util.getCanonicalPath(rrdDef.getPath());
		while (rrdMap.containsKey(canonicalPath) || rrdMap.size() >= capacity) {
			try {
				wait();
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		RrdDb rrdDb = new RrdDb(rrdDef);
		rrdMap.put(canonicalPath, new RrdEntry(rrdDb));
		return rrdDb;
	}

	/* (non-Javadoc)
	 * @see org.rrd4j.core.RrdDbPoolI#requestRrdDb(java.lang.String, java.lang.String)
	 */
	public synchronized RrdDb requestRrdDb(String path, String sourcePath) throws IOException {
		String canonicalPath = Util.getCanonicalPath(path);
		while (rrdMap.containsKey(canonicalPath) || rrdMap.size() >= capacity) {
			try {
				wait();
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		RrdDb rrdDb = new RrdDb(canonicalPath, sourcePath);
		rrdMap.put(canonicalPath, new RrdEntry(rrdDb));
		return rrdDb;
	}

	/* (non-Javadoc)
	 * @see org.rrd4j.core.RrdDbPoolI#release(org.rrd4j.core.RrdDb)
	 */
	public synchronized void release(RrdDb rrdDb) throws IOException {
		// null pointer should not kill the thread, just ignore it
		if (rrdDb == null) {
			return;
		}
		String canonicalPath = Util.getCanonicalPath(rrdDb.getPath());
		if (!rrdMap.containsKey(canonicalPath)) {
			throw new IllegalStateException("Could not release [" + canonicalPath + "], the file was never requested");
		}
		RrdEntry entry = rrdMap.get(canonicalPath);
		if (--entry.count <= 0) {
			// no longer used
			rrdMap.remove(canonicalPath);
			notifyAll();
			entry.rrdDb.close();
		}
	}

	/* (non-Javadoc)
	 * @see org.rrd4j.core.RrdDbPoolI#getCapacity()
	 */
	public synchronized int getCapacity() {
		return capacity;
	}

	/* (non-Javadoc)
	 * @see org.rrd4j.core.RrdDbPoolI#setCapacity(int)
	 */
	public synchronized void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	/* (non-Javadoc)
	 * @see org.rrd4j.core.RrdDbPoolI#getOpenFiles()
	 */
	public synchronized String[] getOpenFiles() {
		return rrdMap.keySet().toArray(new String[0]);
	}

	/* (non-Javadoc)
	 * @see org.rrd4j.core.RrdDbPoolI#getOpenFileCount()
	 */
	public synchronized int getOpenFileCount() {
		return rrdMap.size();
	}

	class RrdEntry {
		RrdDb rrdDb;
		int count;

		RrdEntry(RrdDb rrdDb) {
			this.rrdDb = rrdDb;
			this.count = 1;
		}
	}
}
