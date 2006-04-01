/* ============================================================
 * Rrd4j : Pure java implementation of RRDTool's functionality
 * ============================================================
 *
 * Project Info:  http://www.rrd4j.org
 * Project Lead:  Mathias Bogaert (m.bogaert@memenco.com)
 *
 * (C) Copyright 2003-2006, by Sasa Markovic.
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

/**
 * Class to represent various Rrd4j checked exceptions.
 * Rrd4j code can throw only <code>RrdException</code>
 * (for various Rrd4j related errors) or <code>IOException</code>
 * (for various I/O errors).
 *
 * @author Sasa Markovic
 */
public class RrdException extends Exception {
	/**
	 * Creates new RrdException with the supplied message in it.
	 * @param message Error message.
	 */
	public RrdException(String message) {
		super(message);
	}

	/**
	 * Creates new RrdException object from any java.lang.Exception object
	 * @param e Exception object
	 */
	public RrdException(Exception e) {
		super(e);
	}

}
