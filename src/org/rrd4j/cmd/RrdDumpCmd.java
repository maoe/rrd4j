package org.rrd4j.cmd;

import org.rrd4j.core.RrdDb;

import java.io.IOException;

class RrdDumpCmd extends RrdToolCmd {
    String getCmdType() {
        return "dump";
    }

    Object execute() throws IOException {
        String[] words = getRemainingWords();
        if (words.length != 2) {
            throw new IllegalArgumentException("Invalid rrddump syntax");
        }
        String path = words[1];
        RrdDb rrdDb = getRrdDbReference(path);
        try {
            String xml = rrdDb.getXml();
            println(xml);
            return xml;
        }
        finally {
            releaseRrdDbReference(rrdDb);
        }
    }
}
