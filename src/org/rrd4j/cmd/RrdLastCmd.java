package org.rrd4j.cmd;

import org.rrd4j.core.RrdDb;

import java.io.IOException;

class RrdLastCmd extends RrdToolCmd {

    String getCmdType() {
        return "last";
    }

    Object execute() throws IOException {
        String[] words = getRemainingWords();
        if (words.length != 2) {
            throw new IllegalArgumentException("Invalid rrdlast syntax");
        }
        String path = words[1];
        RrdDb rrdDb = getRrdDbReference(path);
        try {
            long lastUpdateTime = rrdDb.getLastUpdateTime();
            println(lastUpdateTime + "");
            return new Long(lastUpdateTime);
        }
        finally {
            releaseRrdDbReference(rrdDb);
        }
    }
}
