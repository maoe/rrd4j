package org.rrd4j.core;

import org.rrd4j.ConsolFun;

import java.io.IOException;

/**
 * Class to represent single RRD archive in a RRD with its internal state.
 * Normally, you don't need methods to manipulate archive objects directly
 * because Rrd4j framework does it automatically for you.<p>
 * <p/>
 * Each archive object consists of three parts: archive definition, archive state objects
 * (one state object for each datasource) and round robin archives (one round robin for
 * each datasource). API (read-only) is provided to access each of theese parts.<p>
 *
 * @author Sasa Markovic
 */
public class Archive implements RrdUpdater {
    private final RrdDb parentDb;

    // definition
    protected final RrdString consolFun;
    protected final RrdDouble xff;
    protected final RrdInt steps;
    protected final RrdInt rows;

    // state
    private Robin[] robins;
    private ArcState[] states;

    // state for version 2
    private RrdInt[] pointers;
    private RrdDoubleMatrix values;

    Archive(RrdDb parentDb, ArcDef arcDef) throws IOException {
        this.parentDb = parentDb;
        consolFun = new RrdString(this, true);  // constant, may be cached
        xff = new RrdDouble(this);
        steps = new RrdInt(this, true);            // constant, may be cached
        rows = new RrdInt(this, true);            // constant, may be cached
        boolean shouldInitialize = arcDef != null;
        if (shouldInitialize) {
            consolFun.set(arcDef.getConsolFun().name());
            xff.set(arcDef.getXff());
            steps.set(arcDef.getSteps());
            rows.set(arcDef.getRows());
        }
        int n = parentDb.getHeader().getDsCount();
        int numRows = rows.get();
        states = new ArcState[n];
        int version = parentDb.getHeader().getVersion();
        if(version == 1) {
            robins = new RobinArray[n];
        for (int i = 0; i < n; i++) {
            states[i] = new ArcState(this, shouldInitialize);
                robins[i] = new RobinArray(this, numRows, shouldInitialize);
            }
        }
        else {
            pointers = new RrdInt[n];
            robins = new RobinMatrix[n];
            for (int i = 0; i < n; i++) {
                pointers[i]= new RrdInt(this);
                states[i] = new ArcState(this, shouldInitialize);
            }
            values = new RrdDoubleMatrix(this, numRows, n, shouldInitialize);				
            for(int i = 0; i < n; i++) {
                robins[i] = new RobinMatrix(this, values, pointers[i], i);
            }
        }
    }

    // read from XML
    Archive(RrdDb parentDb, DataImporter reader, int arcIndex) throws IOException {
        this(parentDb, new ArcDef(
                reader.getConsolFun(arcIndex), reader.getXff(arcIndex),
                reader.getSteps(arcIndex), reader.getRows(arcIndex)));
        int n = parentDb.getHeader().getDsCount();
        for (int i = 0; i < n; i++) {
            // restore state
            states[i].setAccumValue(reader.getStateAccumValue(arcIndex, i));
            states[i].setNanSteps(reader.getStateNanSteps(arcIndex, i));
            // restore robins
            double[] values = reader.getValues(arcIndex, i);
            robins[i].update(values);
        }
    }

    /**
     * Returns archive time step in seconds. Archive step is equal to RRD step
     * multiplied with the number of archive steps.
     *
     * @return Archive time step in seconds
     * @throws IOException Thrown in case of I/O error.
     */
    public long getArcStep() throws IOException {
        return parentDb.getHeader().getStep() * steps.get();
    }

    String dump() throws IOException {
        StringBuilder builder = new StringBuilder("== ARCHIVE ==\n");
        builder.append("RRA:").append(consolFun.get()).append(":").append(xff.get()).append(":").append(steps.get()).append(":").append(rows.get()).append("\n");
        builder.append("interval [").append(getStartTime()).append(", ").append(getEndTime()).append("]" + "\n");
        for (int i = 0; i < robins.length; i++) {
            builder.append(states[i].dump());
            builder.append(robins[i].dump());
        }
        return builder.toString();
    }

    RrdDb getParentDb() {
        return parentDb;
    }

    void archive(int dsIndex, double value, long numUpdates) throws IOException {
        Robin robin = robins[dsIndex];
        ArcState state = states[dsIndex];
        long step = parentDb.getHeader().getStep();
        long lastUpdateTime = parentDb.getHeader().getLastUpdateTime();
        long updateTime = Util.normalize(lastUpdateTime, step) + step;
        long arcStep = getArcStep();
        // finish current step
        while (numUpdates > 0) {
            accumulate(state, value);
            numUpdates--;
            if (updateTime % arcStep == 0) {
                finalizeStep(state, robin);
                break;
            }
            else {
                updateTime += step;
            }
        }
        // update robin in bulk
        int bulkUpdateCount = (int) Math.min(numUpdates / steps.get(), (long) rows.get());
        robin.bulkStore(value, bulkUpdateCount);
        // update remaining steps
        long remainingUpdates = numUpdates % steps.get();
        for (long i = 0; i < remainingUpdates; i++) {
            accumulate(state, value);
        }
    }

    private void accumulate(ArcState state, double value) throws IOException {
        if (Double.isNaN(value)) {
            state.setNanSteps(state.getNanSteps() + 1);
        }
        else {
            switch (ConsolFun.valueOf(consolFun.get())) {
                case MIN:
                    state.setAccumValue(Util.min(state.getAccumValue(), value));
                    break;
                case MAX:
                    state.setAccumValue(Util.max(state.getAccumValue(), value));
                    break;
                case LAST:
                    state.setAccumValue(value);
                    break;
                case AVERAGE:
                    state.setAccumValue(Util.sum(state.getAccumValue(), value));
                    break;
            }
        }
    }

    private void finalizeStep(ArcState state, Robin robin) throws IOException {
        // should store
        long arcSteps = steps.get();
        double arcXff = xff.get();
        long nanSteps = state.getNanSteps();
        //double nanPct = (double) nanSteps / (double) arcSteps;
        double accumValue = state.getAccumValue();
        if (nanSteps <= arcXff * arcSteps && !Double.isNaN(accumValue)) {
            if (getConsolFun() == ConsolFun.AVERAGE) {
                accumValue /= (arcSteps - nanSteps);
            }
            robin.store(accumValue);
        }
        else {
            robin.store(Double.NaN);
        }
        state.setAccumValue(Double.NaN);
        state.setNanSteps(0);
    }

    /**
     * Returns archive consolidation function ("AVERAGE", "MIN", "MAX" or "LAST").
     *
     * @return Archive consolidation function.
     * @throws IOException Thrown in case of I/O error.
     */
    public ConsolFun getConsolFun() throws IOException {
        return ConsolFun.valueOf(consolFun.get());
    }

    /**
     * Returns archive X-files factor.
     *
     * @return Archive X-files factor (between 0 and 1).
     * @throws IOException Thrown in case of I/O error.
     */
    public double getXff() throws IOException {
        return xff.get();
    }

    /**
     * Returns the number of archive steps.
     *
     * @return Number of archive steps.
     * @throws IOException Thrown in case of I/O error.
     */
    public int getSteps() throws IOException {
        return steps.get();
    }

    /**
     * Returns the number of archive rows.
     *
     * @return Number of archive rows.
     * @throws IOException Thrown in case of I/O error.
     */
    public int getRows() throws IOException {
        return rows.get();
    }

    /**
     * Returns current starting timestamp. This value is not constant.
     *
     * @return Timestamp corresponding to the first archive row
     * @throws IOException Thrown in case of I/O error.
     */
    public long getStartTime() throws IOException {
        long endTime = getEndTime();
        long arcStep = getArcStep();
        long numRows = rows.get();
        return endTime - (numRows - 1) * arcStep;
    }

    /**
     * Returns current ending timestamp. This value is not constant.
     *
     * @return Timestamp corresponding to the last archive row
     * @throws IOException Thrown in case of I/O error.
     */
    public long getEndTime() throws IOException {
        long arcStep = getArcStep();
        long lastUpdateTime = parentDb.getHeader().getLastUpdateTime();
        return Util.normalize(lastUpdateTime, arcStep);
    }

    /**
     * Returns the underlying archive state object. Each datasource has its
     * corresponding ArcState object (archive states are managed independently
     * for each RRD datasource).
     *
     * @param dsIndex Datasource index
     * @return Underlying archive state object
     */
    public ArcState getArcState(int dsIndex) {
        return states[dsIndex];
    }

    /**
     * Returns the underlying round robin archive. Robins are used to store actual
     * archive values on a per-datasource basis.
     *
     * @param dsIndex Index of the datasource in the RRD.
     * @return Underlying round robin archive for the given datasource.
     */
    public Robin getRobin(int dsIndex) {
        return robins[dsIndex];
    }

    FetchData fetchData(FetchRequest request) throws IOException {
        long arcStep = getArcStep();
        long fetchStart = Util.normalize(request.getFetchStart(), arcStep);
        long fetchEnd = Util.normalize(request.getFetchEnd(), arcStep);
        if (fetchEnd < request.getFetchEnd()) {
            fetchEnd += arcStep;
        }
        long startTime = getStartTime();
        long endTime = getEndTime();
        String[] dsToFetch = request.getFilter();
        if (dsToFetch == null) {
            dsToFetch = parentDb.getDsNames();
        }
        int dsCount = dsToFetch.length;
        int ptsCount = (int) ((fetchEnd - fetchStart) / arcStep + 1);
        long[] timestamps = new long[ptsCount];
        double[][] values = new double[dsCount][ptsCount];
        long matchStartTime = Math.max(fetchStart, startTime);
        long matchEndTime = Math.min(fetchEnd, endTime);
        double[][] robinValues = null;
        if (matchStartTime <= matchEndTime) {
            // preload robin values
            int matchCount = (int) ((matchEndTime - matchStartTime) / arcStep + 1);
            int matchStartIndex = (int) ((matchStartTime - startTime) / arcStep);
            robinValues = new double[dsCount][];
            for (int i = 0; i < dsCount; i++) {
                int dsIndex = parentDb.getDsIndex(dsToFetch[i]);
                robinValues[i] = robins[dsIndex].getValues(matchStartIndex, matchCount);
            }
        }
        for (int ptIndex = 0; ptIndex < ptsCount; ptIndex++) {
            long time = fetchStart + ptIndex * arcStep;
            timestamps[ptIndex] = time;
            for (int i = 0; i < dsCount; i++) {
                double value = Double.NaN;
                if (time >= matchStartTime && time <= matchEndTime) {
                    // inbound time
                    int robinValueIndex = (int) ((time - matchStartTime) / arcStep);
                    assert robinValues != null;
                    value = robinValues[i][robinValueIndex];
                }
                values[i][ptIndex] = value;
            }
        }
        FetchData fetchData = new FetchData(this, request);
        fetchData.setTimestamps(timestamps);
        fetchData.setValues(values);
        return fetchData;
    }

    void appendXml(XmlWriter writer) throws IOException {
        writer.startTag("rra");
        writer.writeTag("cf", consolFun.get());
        writer.writeComment(getArcStep() + " seconds");
        writer.writeTag("pdp_per_row", steps.get());
        writer.writeTag("xff", xff.get());
        writer.startTag("cdp_prep");
        for (ArcState state : states) {
            state.appendXml(writer);
        }
        writer.closeTag(); // cdp_prep
        writer.startTag("database");
        long startTime = getStartTime();
        for (int i = 0; i < rows.get(); i++) {
            long time = startTime + i * getArcStep();
            writer.writeComment(Util.getDate(time) + " / " + time);
            writer.startTag("row");
            for (Robin robin : robins) {
                writer.writeTag("v", robin.getValue(i));
            }
            writer.closeTag(); // row
        }
        writer.closeTag(); // database
        writer.closeTag(); // rra
    }

    /**
     * Copies object's internal state to another Archive object.
     *
     * @param other New Archive object to copy state to
     * @throws IOException Thrown in case of I/O error
     */
    public void copyStateTo(RrdUpdater other) throws IOException {
        if (!(other instanceof Archive)) {
            throw new IllegalArgumentException(
                    "Cannot copy Archive object to " + other.getClass().getName());
        }
        Archive arc = (Archive) other;
        if (!arc.consolFun.get().equals(consolFun.get())) {
            throw new IllegalArgumentException("Incompatible consolidation functions");
        }
        if (arc.steps.get() != steps.get()) {
            throw new IllegalArgumentException("Incompatible number of steps");
        }
        int count = parentDb.getHeader().getDsCount();
        for (int i = 0; i < count; i++) {
            int j = Util.getMatchingDatasourceIndex(parentDb, i, arc.parentDb);
            if (j >= 0) {
                states[i].copyStateTo(arc.states[j]);
                robins[i].copyStateTo(arc.robins[j]);
            }
        }
    }

    /**
     * Sets X-files factor to a new value.
     *
     * @param xff New X-files factor value. Must be >= 0 and < 1.
     * @throws IOException Thrown in case of I/O error
     */
    public void setXff(double xff) throws IOException {
        if (xff < 0D || xff >= 1D) {
            throw new IllegalArgumentException("Invalid xff supplied (" + xff + "), must be >= 0 and < 1");
        }
        this.xff.set(xff);
    }

    /**
     * Returns the underlying storage (backend) object which actually performs all
     * I/O operations.
     *
     * @return I/O backend object
     */
    public RrdBackend getRrdBackend() {
        return parentDb.getRrdBackend();
    }

    /**
     * Required to implement RrdUpdater interface. You should never call this method directly.
     *
     * @return Allocator object
     */
    public RrdAllocator getRrdAllocator() {
        return parentDb.getRrdAllocator();
    }
}
