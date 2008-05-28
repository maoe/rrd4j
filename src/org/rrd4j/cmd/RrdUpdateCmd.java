package org.rrd4j.cmd;

import org.rrd4j.core.RrdDb;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;

import java.io.IOException;

class RrdUpdateCmd extends RrdToolCmd {

    String getCmdType() {
        return "update";
    }

    Object execute() throws IOException {
        String template = getOptionValue("t", "template");
        String[] dsNames = (template != null) ? new ColonSplitter(template).split() : null;
        String[] words = getRemainingWords();
        if (words.length < 3) {
            throw new IllegalArgumentException("Insufficent number of parameters for rrdupdate command");
        }
        String path = words[1];
        RrdDb rrdDb = getRrdDbReference(path);
        try {
            if (dsNames != null) {
                // template specified, check datasource names
                for (String dsName : dsNames) {
                    if (!rrdDb.containsDs(dsName)) {
                        throw new IllegalArgumentException("Invalid datasource name: " + dsName);
                    }
                }
            }
            // parse update strings
            long timestamp = -1;
            for (int i = 2; i < words.length; i++) {
                String[] tokens = new ColonSplitter(words[i]).split();
                if (dsNames != null && dsNames.length + 1 != tokens.length) {
                    throw new IllegalArgumentException("Template requires " + dsNames.length + " values, " +
                            (tokens.length - 1) + " value(s) found in: " + words[i]);
                }
                int dsCount = rrdDb.getHeader().getDsCount();
                if (dsNames == null && dsCount + 1 != tokens.length) {
                    throw new IllegalArgumentException("Expected " + dsCount + " values, " +
                            (tokens.length - 1) + " value(s) found in: " + words[i]);
                }
                timestamp = Util.getTimestamp(tokens[0]);
                Sample sample = rrdDb.createSample(timestamp);
                for (int j = 1; j < tokens.length; j++) {
                    if (dsNames == null) {
                        sample.setValue(j - 1, parseDouble(tokens[j]));
                    }
                    else {
                        sample.setValue(dsNames[j - 1], parseDouble(tokens[j]));
                    }
                }
                sample.update();
            }
            return new Long(timestamp);
        }
        finally {
            releaseRrdDbReference(rrdDb);
        }
    }
}

