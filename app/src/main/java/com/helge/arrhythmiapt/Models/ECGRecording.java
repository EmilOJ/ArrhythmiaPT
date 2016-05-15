package com.helge.arrhythmiapt.Models;

import com.jjoe64.graphview.series.DataPoint;
import com.parse.ParseClassName;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ParseClassName("ECGRecording")
public class ECGRecording extends ParseObject {
    /*
        Custom class representing an ECG recording. Also functions as a ParseObject which provides
        methods for syncing with the online database.

        Attributes:
            - data              ECG signal data which is saved as a csv in the database
            - Fs                Sampling frequency
            - downSamplingRate  Factor determining the amount of downsampling for reducing
                                computational load
     */

    private List<Double> mData;
    String sData = "data";
    String sFs = "fs";
    String sDownSamplingRate = "downSamplingRate";

    // Emtpy constructor necessary for implementing the class as a Parse Object
    public ECGRecording() {}

    // Method for getting the ECG signal from the Parse database (csv format) and converting
    // it to a List.
    private void getAndConvertData() {
        String dataString = null;
        try {
            dataString = new String(this.getParseFile(sData).getData());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        List<String> dataPairs = Arrays.asList(dataString.split("\n"));
        mData = new ArrayList<>();

        for (int i=0; i < dataPairs.size(); i++) {
            try {
                mData.add(Double.parseDouble(dataPairs.get(i)));
            } catch (Exception e) {

            }
        }
    }

    // Get all Arrhythmia objects associated with this ECG recording.
    public List<Arrhythmia> getArrhythmias() {
        List<Arrhythmia> aList;
        // Create database query and execute it
        ParseQuery<Arrhythmia> query = new ParseQuery<>(Arrhythmia.class);
        query.whereEqualTo("recordingId", getObjectId());
        try {
            aList = query.find();
        } catch (ParseException e) {
            e.printStackTrace();
            aList = new ArrayList<>();
        }
        return aList;
    }

    // Converts ECG data to a List<DataPoints> where DataPoints is a data type from the GraphView
    // library which is used for plotting.
    // Also downsamples the signal for a more smooth interactive plotting experience.
    public DataPoint[] asDataPoints() {
        if (mData == null) {
            getAndConvertData();
        }
        DataPoint[] dataPointsArray = new DataPoint[(int) Math.floor(mData.size()/getDownSamplingRate())];
        int counter = 0;
        // The iterator i  in the following loope is iterated with the downsampling rate instead
        // of 1.
        for (int i=0; i < mData.size(); i = i + getDownSamplingRate()) {
            double dataPoint = mData.get(i);
            try {
                dataPointsArray[counter] = new DataPoint((counter*getDownSamplingRate())/getFs(), dataPoint);
            } catch (Exception e) {
                int a = 1;
            }

            counter++;
        }

        return dataPointsArray;
    }

    // Returns the maximum signal value. Used for setting y-axis ranges when plotting.
    public double getMax() {
        List<Double> dataMax = new ArrayList<>(mData);
        Collections.sort(dataMax); // Sort the arraylist
        double maxValue = dataMax.get(dataMax.size() - 1);

        return maxValue;
    }

    /* All methods below are trivial getters and setters */

    public List<Double> getData() {
        if (mData == null) {
            getAndConvertData();
        }

        return mData;
    }

    public Double getFs() {
        return getDouble(sFs);
    }

    public void setFs(int Fs) {
        put(sFs, Fs);
    }

    public int getDownSamplingRate() {
        return getInt(sDownSamplingRate);
    }

    public void setDownSamplingRate(int downSamplingRate) {
        put(sDownSamplingRate, downSamplingRate);
    }

    public void setData(ParseFile data) {
        put(sData, data);
    }

}