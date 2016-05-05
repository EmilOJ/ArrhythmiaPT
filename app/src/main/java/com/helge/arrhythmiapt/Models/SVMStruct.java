package com.helge.arrhythmiapt.Models;

import android.content.Context;

import com.helge.arrhythmiapt.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ThereseSchoenemann on 05-05-2016.
 */
public class SVMStruct {
    Context mContext;

    public SVMStruct(Context context) throws IOException  {
        mContext = context;
    }

        // TODO: Get trained SVM classifier weights (trained in MATLAB)
        ArrayList<Double> svm = new ArrayList<Double>();
        //The file is saved in the internal storage , and is found as such:
        InputStream android = mContext.getResources().openRawResource(R.raw.supportvectors);
        //svmAndroid.model file exported from MATLAB when the right parameters have been saved.
        //The file is read:
        BufferedReader reader = new BufferedReader(new InputStreamReader(android));

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] RowData = line.split("\t");
                svm.add(Double.parseDouble(RowData[2])); //insert the right rowData according to the svm from MATLAB
            }
        } catch (IOException ex) {
        } finally {
            try {
                android.close();
            } catch (IOException e) {
            }
        }

    }
