package com.helge.arrhythmiapt.Models;

import android.content.Context;

import com.google.common.primitives.Doubles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ThereseSchoenemann on 05-05-2016.
 */
public class SVMStruct {
    Context mContext;
    private double[][] mSupportVectors;
    private double[] mAlpha;
    double mBias;
    String mArrhythmiaType;

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
        List<ArrayList<Double>> supportVectorsList = new ArrayList<>();
        List<Double> alphaList = new ArrayList<>();

        String pname = mContext.getPackageName();

        int svID = mContext.getResources().getIdentifier("raw/supportvectors_" + mArrhythmiaType, null, pname);
        int alphaID = mContext.getResources().getIdentifier("raw/alpha_" + mArrhythmiaType, null, pname);
        int biasID = mContext.getResources().getIdentifier("raw/bias_" + mArrhythmiaType, null, pname);

        //The file is saved in the internal storage , and is found as such:
        InputStream modelSupportVectors = mContext.getResources().openRawResource(svID);
        InputStream alpha = mContext.getResources().openRawResource(alphaID);
        InputStream bias = mContext.getResources().openRawResource(biasID);
        //The file is read:
        BufferedReader readerSV = new BufferedReader(new InputStreamReader(modelSupportVectors));
        BufferedReader readerAlpha = new BufferedReader(new InputStreamReader(alpha));
        BufferedReader readerBias = new BufferedReader(new InputStreamReader(bias));

        ArrayList<Double> row = new ArrayList<>();
        try {
            String line;
            while ((line = readerSV.readLine()) != null) {
                String[] RowData = line.split("\t");
                for (int i = 0; i <= RowData.length - 1; i++) {
                    row.add(Double.parseDouble(RowData[i]));
                }

                supportVectorsList.add(row);
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
        } catch (IOException ex) {

            } finally {
                try {
                    modelSupportVectors.close();
                } catch (IOException e) {
            }
        }

        mSupportVectors = new double[supportVectorsList.size()][supportVectorsList.get(0).size()];

        for (int i = 0; i < supportVectorsList.size(); i++) {
            mSupportVectors[i] = Doubles.toArray(supportVectorsList.get(i));
        }

        mAlpha = new double[alphaList.size()];
        mAlpha = Doubles.toArray(alphaList);

    }
}
