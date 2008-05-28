package org.rrd4j.cmd;

import org.rrd4j.core.*;
import org.rrd4j.graph.RrdGraphConstants;
import org.rrd4j.ConsolFun;

import java.io.IOException;

class RrdFetchCmd extends RrdToolCmd implements RrdGraphConstants {
    String getCmdType() {
        return "fetch";
    }

    Object execute() throws IOException {
        String startStr = getOptionValue("s", "start", DEFAULT_START);
        String endStr = getOptionValue("e", "end", DEFAULT_END);
        long[] timestamps = Util.getTimestamps(startStr, endStr);
        String resolutionStr = getOptionValue("r", "resolution", "1");
        long resolution = parseLong(resolutionStr);
        // other words
        String[] words = getRemainingWords();
        if (words.length != 3) {
            throw new IllegalArgumentException("Invalid rrdfetch syntax");
        }
        String path = words[1];
        ConsolFun consolFun = ConsolFun.valueOf(words[2]);
        RrdDb rrdDb = getRrdDbReference(path);
        try {
            FetchRequest fetchRequest = rrdDb.createFetchRequest(consolFun, timestamps[0], timestamps[1], resolution);
            System.out.println(fetchRequest.dump());
            FetchData fetchData = fetchRequest.fetchData();
            println(fetchData.toString());
            return fetchData;
        }
        finally {
            releaseRrdDbReference(rrdDb);
        }
    }
}
