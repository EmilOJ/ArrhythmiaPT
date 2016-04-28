package com.helge.arrhythmiapt;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import jwave.Transform;
import jwave.transforms.FastWaveletTransform;
import jwave.transforms.wavelets.Wavelet;
import jwave.transforms.wavelets.WaveletBuilder;
import jwave.transforms.wavelets.biorthogonal.BiOrthogonal35;

public class SignalProcessing extends AppCompatActivity {
    static final String TAG = "SignalProcessing";
    static final int SEGMENT_LENGTH = 200; // ms (on each side of QRS complex)
    static final int NUMBER_OF_FEATURES = 17;
    static final int FS = 320; // Sample rate in Hz
    static final int REFRACTORY_PERIOD = 200; // ms
    // Instance variable containing ECG mSignal
    public ArrayList<Double> mSignal = new ArrayList<Double>();
    public ArrayList<ArrayList<Double>> mSegments = new ArrayList<ArrayList<Double>>();
    public List<Integer> mQrs;
    String waveletName = "BiOrthogonal 3/5";
    Wavelet biorthogonal35 = WaveletBuilder.create(waveletName);

    public void ReadECG(Context context) throws IOException {

        //The file is saved in the internal storage , and is found as such:
//        AssetManager assetManager = context.getResources().getAssets(); a
        InputStream is = context.getResources().openRawResource(R.raw.samples);
        //The file is read:

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] RowData = line.split(",");
                    this.mSignal.add(Double.parseDouble(RowData[2])); //adding mSignal to array
                    //mSignal.add(Double.parseDouble(RowData[1])); //adding time to array??
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
            }
        } catch (IOException ex) {
            Toast toast = Toast.makeText(getApplicationContext(), "ERROR: File not read", Toast.LENGTH_SHORT);
            toast.show();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Toast toast = Toast.makeText(getApplicationContext(), "ERROR: File not closed", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }


    private void detect_and_classify() {
        List<Integer> qrsArray = new ArrayList<Integer>();
        List<ArrayList<Double>> segments = new ArrayList<ArrayList<Double>>();
        List<Double> features = new ArrayList<Double>();
        int detected_qrs;
        int classification;


        // only run QRS detection algorithm if mSignal is longer than refractory period
        if (mSignal.size() > REFRACTORY_PERIOD) {

            // Location (array index) of detected QRS. 0 if no QRS detected.
            int qrs_detected = detect_qrs();

            if (qrs_detected != 0) {
                // Add detected QRS location to previous ones (need 3 for classification)
                mQrs.add(qrs_detected);
                qrsArray.add(qrs_detected);


                // After detecting 3 consequetive QRS complexes
                if (qrsArray.size() == 3) {
                    // Extract ±200 ms mSegments around QRS. This is used for classification.
                    segments = segments_around_qrs(qrsArray);

                    // Compute features
                    features = get_features(segments, qrsArray);

                    // Classify with support vector machine
                    classification = classify_segments(segments, features);

                    // Save classification and mSignal to database
                    save_classification(classification);

                    // TODO: variable 'mSignal' must be emptied like below, so a new QRS detection can be performed,
                    // but the mSignal must also be saved to a variable with continous ECG mSignal containing
                    // all three QRS complexes can be accessed. This is needed for RR interval computation.

                    // Empty mSignal variable
                    this.mSignal = new ArrayList<Double>();


                }
            }
        }

    }

    private int detect_qrs() {
        int foo = 1;
        return foo;
    }


    private ArrayList<ArrayList<Double>> segments_around_qrs(List<Integer> qrsArray) {
        // INPUT:
        //      - mQrs:      location in sample number of the detected mQrs complex
        //      - mSignal:   raw mSignal (or filtered mSignal from detect_qrs()?)
        // OUTPUT:
        //      - mSegments:  The three mSegments consisting of ±200 ms around each QRS complex

        ArrayList<ArrayList<Double>> segments = new ArrayList<ArrayList<Double>>();

        int total_segments_length = SEGMENT_LENGTH * 2 + 1;
        List<Double> segment;
        int pre_qrs, post_qrs, cur_qrs;


        // TODO: Convert to parallel computing? Iterations do not depend on each other.. except adding them to mSegments list.
        // Loop through all mQrs (except first and last)

        for (int j = 0; j < 3; j++) {
            // Find sample index for segment
            cur_qrs = qrsArray.get(j);
            pre_qrs = cur_qrs - SEGMENT_LENGTH;
            post_qrs = cur_qrs + SEGMENT_LENGTH;

            segment = mSignal.subList(pre_qrs, post_qrs);

            segments.add((ArrayList<Double>) segment);
        }

        return segments;
    }


    private List<Double> get_features(List<ArrayList<Double>> segments, List<Integer> qrs) {
        // INPUT:
        //      - mSegments:  Segmentsed mSignal from segments_around_qrs()
        //      - mQrs:  Segmentsed mSignal from segments_around_qrs()
        //      - mSignal:  Segmentsed mSignal from segments_around_qrs()
        // OUPUT:
        //      - features: Computed feature vector


        List<Double> features = new ArrayList<Double>();
        List<Integer> rr_intervals = compute_RR(qrs);

        for (int iSegment = 0; iSegment < segments.size(); iSegment++) {
            // Only use middle segment
            List<Double> segment = segments.get(1);
            double[] segmentArray = asArray(segments.get(iSegment));

            double K = 267; //Estimate since in Song (2005) they have a fs = 360 and K=300
            //We have a fs = 320, 40/360 = 0.111, 0.111*300 is 33.33, 300-33.33 = 266.66

            // Feature 1
            features.add(K / rr_intervals.get(0));
            // Feature 2
            features.add(K / rr_intervals.get(1));

            // Feauture 3-17
            // TODO: Implement wavelet transform from Jwave.
            double[] wavelet_coefficients = new double[segment.size()];
            Transform t = new Transform(new FastWaveletTransform(new BiOrthogonal35()));
            wavelet_coefficients = t.forward(segmentArray);


            // TODO: Set features 3-17 to wavelet coefficients
            //wavelet_coefficients[1];
        }


        return features;
    }

    private List<Integer> compute_RR(List<Integer> qrs) {
        // INPUT:
        //      - qrs:  Segmentsed signal from segments_around_qrs()
        // OUPUT:
        //      - rr_intervals: Computed feature vector
        List<Integer> rr_intervals = new ArrayList<Integer>();

        // TODO: Computer RR-intervals


        return rr_intervals;
    }

    private int classify_segments(List<ArrayList<Double>> segments, List<Double> features) {
        // INPUT:
        //      - mSegments:
        //      - features:
        // OUTPUT:
        //      - mSegments:  The three mSegments consisting of ±200 ms around each QRS complex
        int classification;

        // Only use middle segment
        List<Double> segment = segments.get(1);


        // TODO: Get trained SVM classifier weights (trained in MATLAB)
        // TODO: Implement SVM classification
//        classification = svm_classify(segment, features);
        classification = 0;

        return classification;
    }

    private void save_classification(int classification) {
        // INPUT:
        //      - classfication:    from classify_segments()
        // Saves classification to database

        // TODO: Save classification to database
        // TDOD: Save mSignal mSegments to database or maybe just QRS locations?

    }

    private double[] asArray(ArrayList<Double> arrayList) {

        double[] doubleArray = new double[arrayList.size()]; //create an array with the size of the mSegments

        for (int i = 0; i < mSegments.size(); i++) {
            doubleArray[i] = arrayList.get(i);
        }
        return doubleArray;
    }

}
