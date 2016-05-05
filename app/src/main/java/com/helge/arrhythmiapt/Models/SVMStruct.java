package com.helge.arrhythmiapt.Models;

import android.content.Context;
import com.helge.arrhythmiapt.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Created by ThereseSchoenemann on 05-05-2016.
 */
public class SVMStruct {
    Context mContext;
    double[][] mSupportVectors;
    double[] mAlpha;
    double mBias;

    public double[][] getSupportVectors() {
        return mSupportVectors;
    }

    public double[] getAlpha() {
        return mAlpha;
    }

    public double getBias() {
        return mBias;
    }

    public SVMStruct(Context context) {
        mContext = context;
    }
        public void loadSVMStruct () throws IOException{
            // TODO: Get trained SVM classifier weights (trained in MATLAB)
            //The file is saved in the internal storage , and is found as such:
            InputStream modelSupportVectors = mContext.getResources().openRawResource(R.raw.supportvectors);
            InputStream alpha = mContext.getResources().openRawResource(R.raw.alpha);
            InputStream bias = mContext.getResources().openRawResource(R.raw.bias);
            //The file is read:
            BufferedReader readerSV = new BufferedReader(new InputStreamReader(modelSupportVectors));
            BufferedReader readerAlpha = new BufferedReader(new InputStreamReader(alpha));
            BufferedReader readerBias = new BufferedReader(new InputStreamReader(bias));
            try {
                String line;
                int row = 0;
                while ((line = readerSV.readLine()) != null) {
                    String[] RowData = line.split("\t");
                    for (int i = 0; i<=RowData.length-1;  i++){
                        mSupportVectors[row][i]= Double.parseDouble(RowData[i]);
                    }
                }
                while ((line = readerAlpha.readLine()) != null) {
                    String[] DataAlpha = line.split("\t");
                    mAlpha[1] = Double.parseDouble(DataAlpha[1]);
                }
                while((line = readerBias.readLine()) != null){
                    String[] DataBias = line.split("\t");
                    mBias= Double.parseDouble(DataBias[1]);
                }
            } catch (IOException ex) {
            } finally {
                try {
                    modelSupportVectors.close();
                } catch (IOException e) {
                }
            }
    }
    }
