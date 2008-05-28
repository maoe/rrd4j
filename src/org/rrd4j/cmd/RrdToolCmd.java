package org.rrd4j.cmd;

import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDbPool;
import org.rrd4j.core.RrdDef;

import java.io.IOException;

abstract class RrdToolCmd {

    private RrdCmdScanner cmdScanner;

    abstract String getCmdType();

    abstract Object execute() throws IOException;

    Object executeCommand(String command) throws IOException {
        cmdScanner = new RrdCmdScanner(command);
        return execute();
    }

    String getOptionValue(String shortForm, String longForm) {
        return cmdScanner.getOptionValue(shortForm, longForm);
    }

    String getOptionValue(String shortForm, String longForm, String defaultValue) {
        return cmdScanner.getOptionValue(shortForm, longForm, defaultValue);
    }

    String[] getMultipleOptionValues(String shortForm, String longForm) {
        return cmdScanner.getMultipleOptions(shortForm, longForm);
    }

    boolean getBooleanOption(String shortForm, String longForm) {
        return cmdScanner.getBooleanOption(shortForm, longForm);
    }

    String[] getRemainingWords() {
        return cmdScanner.getRemainingWords();
    }

    static boolean rrdDbPoolUsed = true;
    static boolean standardOutUsed = true;

    static boolean isRrdDbPoolUsed() {
        return rrdDbPoolUsed;
    }

    static void setRrdDbPoolUsed(boolean rrdDbPoolUsed) {
        RrdToolCmd.rrdDbPoolUsed = rrdDbPoolUsed;
    }

    static boolean isStandardOutUsed() {
        return standardOutUsed;
    }

    static void setStandardOutUsed(boolean standardOutUsed) {
        RrdToolCmd.standardOutUsed = standardOutUsed;
    }

    static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(nfe);
        }
    }

    static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(nfe);
        }
    }

    static double parseDouble(String value) {
        if (value.equals("U")) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(nfe);
        }
    }

    static void print(String s) {
        if (standardOutUsed) {
            System.out.print(s);
        }
    }

    static void println(String s) {
        if (standardOutUsed) {
            System.out.println(s);
        }
    }

    static RrdDb getRrdDbReference(String path) throws IOException {
        if (rrdDbPoolUsed) {
            return RrdDbPool.getInstance().requestRrdDb(path);
        }
        else {
            return new RrdDb(path);
        }
    }

    static RrdDb getRrdDbReference(String path, String xmlPath) throws IOException {
        if (rrdDbPoolUsed) {
            return RrdDbPool.getInstance().requestRrdDb(path, xmlPath);
        }
        else {
            return new RrdDb(path, xmlPath);
        }
    }

    static RrdDb getRrdDbReference(RrdDef rrdDef) throws IOException {
        if (rrdDbPoolUsed) {
            return RrdDbPool.getInstance().requestRrdDb(rrdDef);
        }
        else {
            return new RrdDb(rrdDef);
        }
    }

    static void releaseRrdDbReference(RrdDb rrdDb) throws IOException {
        if (rrdDbPoolUsed) {
            RrdDbPool.getInstance().release(rrdDb);
        }
        else {
            rrdDb.close();
        }
    }
}
