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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RrdDbPoolNew extends RrdDbPool {
	static private class Indirection {
		RrdDb db = null;
		int count = 0;
	}
	private Semaphore capacity;
	private int maxCapacity = INITIAL_CAPACITY;

	static private final ConcurrentHashMap<String, Indirection> pool = new ConcurrentHashMap<String, Indirection>(INITIAL_CAPACITY);

	private static final AtomicLong waitTime = new AtomicLong(0);
	private static final AtomicInteger lockCount = new AtomicInteger(0);

	protected RrdDbPoolNew() {
		RrdBackendFactory factory = RrdBackendFactory.getDefaultFactory();

		if (!(factory instanceof RrdFileBackendFactory)) {
			throw new RuntimeException("Cannot create instance of " + getClass().getName() + " with " +
			"a default backend factory not derived from RrdFileBackendFactory");
		}
		capacity = new Semaphore(maxCapacity);
	}

	public int getOpenFileCount() {
		return pool.size();
	}

	public String[] getOpenFiles() {
		return pool.keySet().toArray(new String[0]);
	}

	public void release(RrdDb rrdDb) throws IOException {
		long start = System.currentTimeMillis();
		lockCount.incrementAndGet();
		Indirection ref = pool.get(rrdDb.getCanonicalPath());
		if(ref != null) {
			synchronized(ref) {
				if(ref.db != null) {
					ref.count--;
					if(ref.count <= 0) {
						ref.db.close();
						ref.db = null;
						pool.remove(ref);
						capacity.release();
					}
				}
			}
		}
		long finish = System.currentTimeMillis();
		waitTime.addAndGet(finish - start);
	}

	public RrdDb requestRrdDb(String path) throws IOException {
		String canonicalPath = Util.getCanonicalPath(path);
		long start = System.currentTimeMillis();
		lockCount.incrementAndGet();
		Indirection ref = pool.putIfAbsent(canonicalPath, new Indirection());
		if(ref == null)
			ref = pool.get(canonicalPath);
		synchronized(ref) {
			if(ref.db == null) {
				capacity.tryAcquire();
				ref.db = new RrdDb(path);
			}
			ref.count++;
		}
		long finish = System.currentTimeMillis();
		waitTime.addAndGet(finish - start);
		return ref.db;
	}

	public RrdDb requestRrdDb(RrdDef rrdDef) throws IOException {
		String canonicalPath = Util.getCanonicalPath(rrdDef.getPath());
		long start = System.currentTimeMillis();
		lockCount.incrementAndGet();
		Indirection ref = pool.putIfAbsent(canonicalPath, new Indirection());
		if(ref == null)
			ref = pool.get(canonicalPath);
		synchronized(ref) {
			if(ref.db == null) {
				capacity.tryAcquire();
				ref.db = new RrdDb(rrdDef);
			}
			ref.count++;
		}
		long finish = System.currentTimeMillis();
		waitTime.addAndGet(finish - start);
		return ref.db;
	}

	public RrdDb requestRrdDb(String path, String sourcePath)
	throws IOException {
		String canonicalPath = Util.getCanonicalPath(path);
		long start = System.currentTimeMillis();
		lockCount.incrementAndGet();
		Indirection ref = pool.putIfAbsent(canonicalPath, new Indirection());
		if(ref == null)
			ref = pool.get(canonicalPath);
		synchronized(ref) {
			if(ref.db == null) {
				capacity.tryAcquire();
				ref.db = new RrdDb(path, sourcePath);
			}
			ref.count++;
		}
		long finish = System.currentTimeMillis();
		waitTime.addAndGet(finish - start);
		return ref.db;
	}

	public void setCapacity(int newCapacity) {
		int available  = capacity.drainPermits();
		if(available != maxCapacity) {
			capacity.release(available);
			throw new RuntimeException("Can only be done on a empty pool");
		}
		else {
			capacity = new Semaphore(newCapacity);
		}

	}
	public int getCapacity() {
		return maxCapacity;
	}


}
