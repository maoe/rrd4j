package org.rrd4j.cmd;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Class to be used to execute various RRDTool commands (original syntax of RRDTool 1.0.x must be used).
 * Currently supported commands are CREATE, UPDATE, LAST, FETCH, DUMP, RESTORE, XPORT, GRAPH, TUNE, INFO
 */
public class RrdCommander {
    private static final RrdToolCmd[] rrdCommands = {
            new RrdCreateCmd(),
            new RrdUpdateCmd(),
            new RrdLastCmd(),
            new RrdFetchCmd(),
            new RrdDumpCmd(),
            new RrdRestoreCmd(),
            new RrdXportCmd(),
            new RrdGraphCmd(),
            new RrdTuneCmd(),
            new RrdInfoCmd()
    };

    /**
     * Checks if the output from any RRDTool command will be visible on the standard output device
     * (console). Default setting is <code>true</code>.
     *
     * @return true, if the output will be visible on the standard output device; false, otherwise.
     */
    public static synchronized boolean isStandardOutUsed() {
        return RrdToolCmd.isStandardOutUsed();
    }

    /**
     * Method used to control access to stdout (System.out, console) for all RRDTool commands. By default,
     * all RRDTool commands are allowed to print results to stdout, in a form used by RRDTool.
     *
     * @param standardOutUsed <code>true</code> if the output should be visible on the
     *                        standard output device, <code>false</code> otherwise.
     */
    public static synchronized void setStandardOutUsed(boolean standardOutUsed) {
        RrdToolCmd.setStandardOutUsed(standardOutUsed);
    }

    /**
     * Checks if the class uses {@link org.rrd4j.core.RrdDbPool} internally while executing
     * RRDTool commands.
     *
     * @return true if the pool is used, false otherwise
     */
    public static synchronized boolean isRrdDbPoolUsed() {
        return RrdToolCmd.isRrdDbPoolUsed();
    }

    /**
     * Forces or prohibits {@link org.rrd4j.core.RrdDbPool} usage internally while executing
     * RRDTool commands
     *
     * @param rrdDbPoolUsed true, to force pool usage, false otherwise.
     */
    public static synchronized void setRrdDbPoolUsed(boolean rrdDbPoolUsed) {
        RrdToolCmd.setRrdDbPoolUsed(rrdDbPoolUsed);
    }

    /**
     * Executes single RRDTool command. The command string should start with some
     * well known RRDTool command word (create, update, fetch, graph...)<p>
     *
     * @param command RRDTool command like: <p>
     *                <pre>
     *                create test.rrd --start "noon yesterday" --step 300 DS:x:GAUGE:600:U:U RRA:AVERAGE:0.5:5:1000
     *                update test.rrd N:1000
     *                last test.rrd
     *                ...
     *                </pre>
     * @return Result of specific RRDTool command. It is guaranteed that the result of any
     *         successfully executed command will be always different from null.
     *         Unsuccessfully executed commands will always throw
     *         an exception, so you need not check for null results.<p>
     *         Exact type of the result depends from the
     *         type of executed RRDTool command:<p>
     *         <ul>
     *         <li><b>create</b>: returns java.lang.String containing path to the newly created RRD file.
     *         <li><b>last</b>: returns java.lang.Long representing timestamp of the last update.
     *         <li><b>update</b>: returns java.lang.Long representing timestamp of the last update.
     *         <li><b>dump</b>: returns (very long) java.lang.String representing the content of a RRD file
     *         in XML format.
     *         <li><b>fetch</b>: returns {@link org.rrd4j.core.FetchData} object representing fetched data.
     *         <li><b>restore</b>: returns path to the restored RRD file.
     *         <li><b>xport</b>: returns java.lang.String containing exported data
     *         <li><b>graph</b>: returns {@link org.rrd4j.graph.RrdGraphInfo} object containing graph info
     *         <li><b>tune</b>: returns path to the tuned RRD file
     *         </ul>
     * @throws IOException              thrown in case of I/O error
     * @throws IllegalArgumentException thrown for all other errors (parsing errors,
     *                                  unknown RRDTool syntax/command/option, internal RRD errors...)
     */
    public static synchronized Object execute(String command) throws IOException {
        String cmd = command.trim(), rrdtool = "rrdtool ";
        if (cmd.startsWith(rrdtool)) {
            cmd = cmd.substring(rrdtool.length());
        }
        for (RrdToolCmd rrdCommand : rrdCommands) {
            if (cmd.startsWith(rrdCommand.getCmdType() + " ")) {
                return rrdCommand.executeCommand(cmd);
            }
        }
        throw new IllegalArgumentException("Unknown RRDTool command: " + command);
    }

    /**
     * A small demo which allows you to pass arbitrary RRDTool commands to Rrd4j
     *
     * @param args Not used
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        System.out.println("== Rrd4j's RRDTool commander ==");
        System.out.println("Type a RRDTool command after the dollar sign and press Enter.");
        System.out.println("Start your RRDTool command with 'create', 'update', 'fetch' etc.");
        System.out.println("Start line with 'create', 'update', 'fetch' etc.");
        System.out.println("Enter dot ('.') to bail out");
        System.out.println("Current directory is: " + new File(".").getCanonicalPath());
        System.out.println("================================");
        RrdToolCmd.setRrdDbPoolUsed(false);
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                System.out.print("$ ");
                String s = r.readLine().trim();
                if (s.length() > 0) {
                    if (!s.startsWith(".")) {
                        execute(s);
                    }
                    else {
                        break;
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
}
