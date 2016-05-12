package com.helge.arrhythmiapt.Models;

import com.parse.ParseClassName;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

@ParseClassName("Arrhythmia")
public class Arrhythmia extends ParseObject {
    /*
        Custom class representing an arrhythmia. Also functions as a ParseObject which provides
        methods for syncing with the online database.

        Attributes:
            - start         (position in ECG signal)
            - stop          (position in ECG signal)
            - duration      (stop - start)
            - type          (e.g. "AF" for atrial fibrillation)
            - recordingID   (id of the corresponding ECG recording - foreign key)
     */

    String sStop = "stop";
    String sStart = "start";
    String sDuration = "duration";
    String sType = "type";
    String sRecordingId = "recordingId";

    // Emtpy constructor necessary for implementing the class as a Parse Object
    public Arrhythmia() {
    }

    public Arrhythmia(int start, int stop) {
        super("Arrhythmia");
        this.put(sStart, start);
        this.put(sStop, stop);
        this.put(sDuration, computeDuration());
    }

    public Arrhythmia(int start, int stop, String type) {
        super("Arrhythmia");
        this.put(sStart, start);
        this.put(sStop, stop);
        this.put(sType, type);
        this.put(sDuration, computeDuration());
    }

    // Returns the ECGRecording object associated with this Arrhythmia
    // (specified by "recordingID")
    public ECGRecording getECGRecording() {
        ECGRecording ecg;
        // Create database query and run it
        ParseQuery<ECGRecording> query = new ParseQuery(ECGRecording.class);
        query.fromLocalDatastore();
        query.whereEqualTo("objectId", getRecordingID());
        try {
            ecg = query.getFirst();
        } catch (ParseException e) {
            ecg = new ECGRecording();
        }

        return ecg;
    }

    // Utility function which computes the arrhythmia duration from the start and stop time
    private double computeDuration() {
        double duration;
        duration = this.getDouble(sStop) - this.getDouble(sStart);
        return duration;
    }

    // Convert the start time in samples to seconds
    public double getStartTime() {
        return this.getStart() / this.getECGRecording().getFs();
    }

    // Convert the stop time in samples to seconds
    public double getStopTime() {
        return this.getStop() / this.getECGRecording().getFs();
    }


    /* All methods below are trivial getters and setters */

    public String getRecordingID() {
        return getString(sRecordingId);
    }

    public void setRecordingId(String recordingId) {
        put(sRecordingId, recordingId);
    }

    public int getStop() {
        return getInt(sStop);
    }

    public void setStop(double stop) {
        put(sStop, stop);
    }

    public int getStart() {
        return getInt(sStart);
    }

    public void setStart(double start) {
        put(sStart, start);
    }

    public int getDuration() {
        return getInt(sDuration);
    }

    public void setDuration(double duration) {
        put(sDuration, duration);
    }

    public String getType() {
        return getString(sType);
    }

    public void setType(String type) {
        put(sType, type);
    }
}
