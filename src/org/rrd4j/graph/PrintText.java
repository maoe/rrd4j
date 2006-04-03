/* ============================================================
 * Rrd4j : Pure java implementation of RRDTool's functionality
 * ============================================================
 *
 * Project Info:  http://www.rrd4j.org
 * Project Lead:  Mathias Bogaert (m.bogaert@memenco.com)
 *
 * Developers:    Sasa Markovic
 *
 *
 * (C) Copyright 2003-2006, by Sasa Markovic.
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
package org.rrd4j.graph;

import org.rrd4j.ConsolFun;
import org.rrd4j.core.Util;
import org.rrd4j.data.DataProcessor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PrintText extends CommentText {
	static final String UNIT_MARKER = "([^%]?)%(s|S)";
	static final Pattern UNIT_PATTERN = Pattern.compile(UNIT_MARKER);

	private final String srcName;
    private ConsolFun consolFun;
	private final boolean includedInGraph;

	PrintText(String srcName, ConsolFun consolFun, String text, boolean includedInGraph) {
		super(text);
		this.srcName = srcName;
		this.consolFun = consolFun;
		this.includedInGraph = includedInGraph;
	}

	boolean isPrint() {
		return !includedInGraph;
	}

	void resolveText(DataProcessor dproc, ValueScaler valueScaler) {
		super.resolveText(dproc, valueScaler);
		if (resolvedText != null) {
			double value = dproc.getAggregate(srcName, consolFun);
			Matcher matcher = UNIT_PATTERN.matcher(resolvedText);
			if(matcher.find()) {
				// unit specified
				ValueScaler.Scaled scaled = valueScaler.scale(value, matcher.group(2).equals("s"));
				resolvedText = resolvedText.substring(0, matcher.start()) +
						matcher.group(1) + scaled.unit + resolvedText.substring(matcher.end());
				value = scaled.value;
			}
			resolvedText = Util.sprintf(resolvedText, value);
			trimIfGlue();
		}
	}
}
