package com.helge.arrhythmiapt.Models;

import com.parse.ParseClassName;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

/**
 * Created by emil on 26/04/16.
 */
@ParseClassName("Arrhythmia")
public class Arrhythmia extends ParseObject {
    String sStop = "stop";
    String sStart = "start";
    String sDuration = "duration";
    String sType = "type";
    String sRecordingId = "recordingId";

    public Arrhythmia() {
    }

    public Arrhythmia(int start, int stop) {
        super("Arrythmia");
        this.put(sStart, start);
        this.put(sStop, stop);
        this.put(sDuration, computeDuration());
    }

    public Arrhythmia(int start, int stop, String type) {
        super("Arrythmia");
        this.put(sStart, start);
        this.put(sStop, stop);
        this.put(sType, type);
        this.put(sDuration, computeDuration());
    }

    public String getRecordingID() {
        return getString(sRecordingId);
    }

    public void setRecordingId(String recordingId) {
        put(sRecordingId, recordingId);
    }

    private double computeDuration() {
        double duration;
        duration = this.getDouble(sStop) - this.getDouble(sStart);
        return duration;
    }

    public ECGRecording getECGRecoridng () {
        ECGRecording ecg;
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

    public int getStop() {
        return getInt(sStop);
    }

    public void setStop(double stop) {
        put(sStop, stop);
    }

    public double getStopTime() {
        return this.getStop() / this.getECGRecoridng().getFs();
    }

    public double getStartTime() {
        return this.getStart() / this.getECGRecoridng().getFs();
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
