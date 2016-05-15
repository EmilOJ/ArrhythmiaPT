package com.helge.arrhythmiapt.Models;

import android.content.Context;

import com.google.common.primitives.Doubles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SVMStruct {
    /*
        Class representing a support vector machine classifier. The support vectors, bias and
        other parameters are saved locally, and this class is simply an interface for loading the
        parameters and returning them in a convenient way.
     */
    Context mContext;
    private double[][] mSupportVectors;
    private double[] mAlpha;
    private double[] mShift;
    private double[] mScaleFactor;
    double mBias;
    String mArrhythmiaType;

    // Contstructor. If more SVMs are present, the type can be specified.
    public SVMStruct(Context context, String arrhythmiaType) {
        mContext = context;
        mArrhythmiaType = arrhythmiaType;

        try {
            loadSVMStruct();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSVMStruct() throws IOException {
        // Loads the SVM structure from local data storage.

        List<ArrayList<Double>> supportVectorsList = new ArrayList<>();
        List<Double> alphaList = new ArrayList<>();
        List<Double> shiftList = new ArrayList<>();
        List<Double> scaleFactorList = new ArrayList<>();
        String pname = mContext.getPackageName();
        int svID = mContext.getResources().getIdentifier("raw/supportvectors", null, pname);
        int alphaID = mContext.getResources().getIdentifier("raw/alpha", null, pname);
        int biasID = mContext.getResources().getIdentifier("raw/bias", null, pname);
        int shiftID = mContext.getResources().getIdentifier("raw/shift", null, pname);
        int scaleFactorID = mContext.getResources().getIdentifier("raw/scalefactor", null, pname);


        InputStream modelSupportVectors = mContext.getResources().openRawResource(svID);
        InputStream alpha = mContext.getResources().openRawResource(alphaID);
        InputStream bias = mContext.getResources().openRawResource(biasID);
        InputStream shift = mContext.getResources().openRawResource(shiftID);
        InputStream scaleFactor = mContext.getResources().openRawResource(scaleFactorID);

        // The file is read:
        BufferedReader readerSV = new BufferedReader(new InputStreamReader(modelSupportVectors));
        BufferedReader readerAlpha = new BufferedReader(new InputStreamReader(alpha));
        BufferedReader readerBias = new BufferedReader(new InputStreamReader(bias));
        BufferedReader readerShift = new BufferedReader(new InputStreamReader(shift));
        BufferedReader readerScaleFactor = new BufferedReader(new InputStreamReader(scaleFactor));

        // Convert files to java data types
        ArrayList<Double> row = new ArrayList<>();
        try {
            String line;
            while ((line = readerSV.readLine()) != null) {
                String[] RowData = line.split("\t");
                for (int i = 0; i <= RowData.length - 1; i++) {
                    row.add(Double.parseDouble(RowData[i]));
                }

                supportVectorsList.add(new ArrayList<Double>(row));
                row.clear();
            }
            while ((line = readerAlpha.readLine()) != null) {
                String[] DataAlpha = line.split("\t");
                alphaList.add(Double.parseDouble(DataAlpha[0]));
            }
            while ((line = readerBias.readLine()) != null) {
                String[] DataBias = line.split("\t");
                mBias = Double.parseDouble(DataBias[0]);
            }
            while ((line = readerShift.readLine()) != null) {
                String[] DataAlpha = line.split("\t");
                shiftList.add(Double.parseDouble(DataAlpha[0]));
            }
            while ((line = readerScaleFactor.readLine()) != null) {
                String[] DataAlpha = line.split("\t");
                scaleFactorList.add(Double.parseDouble(DataAlpha[0]));
            }
        } catch (IOException ex) {

            } finally {
                try {
                    modelSupportVectors.close();
                } catch (IOException e) {
                    e.printStackTrace();
            }
        }

        // Convert to primitive types
        mSupportVectors = new double[supportVectorsList.size()][supportVectorsList.get(0).size()];

        for (int i = 0; i < supportVectorsList.size(); i++) {
            mSupportVectors[i] = Doubles.toArray(supportVectorsList.get(i));
        }

        mAlpha = new double[alphaList.size()];
        mAlpha = Doubles.toArray(alphaList);

        mShift = new double[shiftList.size()];
        mShift = Doubles.toArray(shiftList);

        mScaleFactor = new double[scaleFactorList.size()];
        mScaleFactor = Doubles.toArray(scaleFactorList);
    }

    public int getNumberOfVectors() {
        return getSupportVectors()[0].length;
    }

    public double[][] getSupportVectors() {
        return mSupportVectors;
    }

    public double[] getAlpha() {
        return mAlpha;
    }

    public double getBias() {
        return mBias;
    }

    public double[] getShift() {
        return mShift;
    }

    public double[] getScaleFactor() {
        return mScaleFactor;
    }
}
